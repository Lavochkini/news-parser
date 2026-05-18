package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LinguisticAnalyzer {

    private static final Set<String> HEDGE_WORDS = Set.of(
            "allegedly", "reportedly", "apparently", "seemingly", "supposedly",
            "claimed", "unconfirmed", "purportedly", "rumored", "speculated",
            "possibly", "perhaps", "maybe", "might", "uncertain", "unclear",
            "unverified", "believed", "thought", "suggested", "implied",
            "according to sources", "sources say", "anonymous sources",
            "said to be", "it is claimed", "it is believed"
    );

    private static final Set<String> CLICKBAIT_WORDS = Set.of(
            "shocking", "unbelievable", "mind-blowing", "jaw-dropping",
            "you won't believe", "secret", "revealed", "exposed",
            "breaking", "exclusive", "incredible", "viral", "trending",
            "the truth about", "what they don't want you to know",
            "this is why", "here's why", "find out",
            "must read", "wake up", "share this",
            "mainstream media won't", "watch before deleted",
            "they're hiding", "the real truth", "bombshell"
    );

    private static final Set<String> EMOTIONAL_WORDS = Set.of(
            "catastrophe", "catastrophic", "disaster", "devastating", "crisis",
            "chaos", "terror", "panic", "outrage", "fury", "rage",
            "scandal", "explosive", "horrifying", "alarming", "appalling",
            "atrocious", "dreadful", "disgusting", "vile", "monstrous",
            "propaganda", "manipulation", "censorship", "cover-up", "conspiracy",
            "corrupt", "corruption", "treasonous", "treason", "criminal",
            "witch hunt", "rigged", "stolen", "fraud", "hoax",
            "radical", "extreme", "extremist", "destroy", "destroying"
    );

    // Патерни маніпулятивної політичної риторики
    private static final List<Pattern> MANIPULATION_PATTERNS = List.of(
            // "fake news" / "lying media"
            pattern("(fake|lying|biased|corrupt) (news|media|press|journalist)"),
            // "BREAKING:" у всіх великих + political target
            pattern("BREAKING[:\\s].{0,60}(obama|clinton|trump|biden|democrat|republican)"),
            // "caught red-handed / caught lying"
            pattern("caught (red.handed|lying|cheating|stealing|committing)"),
            // Узагальнення без джерел: "everyone knows", "all true patriots"
            pattern("(everyone knows|all true (patriots|americans)|real (patriots|americans))"),
            // "globalist agenda / socialist plot"
            pattern("(globalist|socialist|communist|marxist) (agenda|plot|takeover|scheme)"),
            // "will destroy America/the country"
            pattern("will (destroy|ruin|end) (america|the country|our nation|democracy)")
    );

    private static final int MANIPULATION_PENALTY = 8; // за кожен знайдений патерн, макс 16

    // Конспірологічні патерни — regex на заголовку
    private static final List<Pattern> CONSPIRACY_PATTERNS = List.of(
            // "X is/might be/could be alive"
            pattern("(is|might be|could be|still|actually) alive"),
            // "faked his/her/their death"
            pattern("faked (his|her|their|the)? ?death"),
            // "never died", "didn't really die"
            pattern("never (really )?died|didn'?t (really )?die"),
            // "was secretly replaced / is a clone"
            pattern("(secret(ly)?|actually) (replaced|cloned|a clone|a double)"),
            // "crisis actor / false flag"
            pattern("crisis actor|false flag"),
            // Відомі конспірації
            pattern("illuminati|lizard (people|person)|flat ?earth|new world order"),
            pattern("chemtrail|microchip(ped)?|5g (causes|kills|spread)"),
            // "what (they/the government/media) don't want you to know/see"
            pattern("(they|government|media|cdc|who) (don'?t|doesn'?t) want you to (know|see|hear)"),
            // "hidden truth / suppressed / banned video"
            pattern("(hidden|suppressed|banned|forbidden) (truth|cure|video|technology)"),
            // "government hiding / deep state plot"
            pattern("government (is )?hiding|deep state (plot|agenda|conspiracy)")
    );

    private static final int MAX_SCORE               = 35;
    private static final int MAX_HEDGE_PENALTY       = 15;
    private static final int MAX_CLICKBAIT_PENALTY   = 10;
    private static final int MAX_EMOTIONAL_PENALTY   = 10;
    private static final int MAX_MANIPULATION_PENALTY= 16;
    private static final int CONSPIRACY_PENALTY      = 20; // фіксований штраф

    public LinguisticResult analyze(NewsItem item) {
        String fullText  = merge(item.getTitle(), item.getDescription(), item.getFullContent()).toLowerCase();
        String titleOnly = item.getTitle() != null ? item.getTitle().toLowerCase() : "";

        List<String> hedgeFound = HEDGE_WORDS.stream()
                .filter(fullText::contains)
                .collect(Collectors.toList());

        List<String> emotionalFound = EMOTIONAL_WORDS.stream()
                .filter(fullText::contains)
                .collect(Collectors.toList());

        List<String> clickbaitFound = new ArrayList<>(CLICKBAIT_WORDS.stream()
                .filter(titleOnly::contains)
                .collect(Collectors.toList()));

        if (item.getTitle() != null && item.getTitle().matches(".*[!?]{2,}.*"))
            clickbaitFound.add("excessive punctuation");
        if (item.getTitle() != null && item.getTitle().matches(".*\\b[A-Z]{4,}\\b.*"))
            clickbaitFound.add("ALL CAPS word");

        // Маніпулятивні політичні патерни — шукаємо у повному тексті
        List<String> manipulationFound = MANIPULATION_PATTERNS.stream()
                .filter(p -> p.matcher(fullText).find())
                .map(Pattern::pattern)
                .collect(Collectors.toList());

        // Конспірологічні патерни — шукаємо в заголовку і описі
        String titleAndDesc = merge(item.getTitle(), item.getDescription()).toLowerCase();
        List<String> conspiracyFound = CONSPIRACY_PATTERNS.stream()
                .filter(p -> p.matcher(titleAndDesc).find())
                .map(Pattern::pattern)
                .collect(Collectors.toList());

        int penalty = 0;
        penalty += Math.min(hedgeFound.size()        * 3, MAX_HEDGE_PENALTY);
        penalty += Math.min(clickbaitFound.size()    * 5, MAX_CLICKBAIT_PENALTY);
        penalty += Math.min(emotionalFound.size()    * 2, MAX_EMOTIONAL_PENALTY);
        penalty += Math.min(manipulationFound.size() * 8, MAX_MANIPULATION_PENALTY);
        if (!conspiracyFound.isEmpty()) penalty += CONSPIRACY_PENALTY;

        int score = Math.max(MAX_SCORE - penalty, 0);

        return LinguisticResult.builder()
                .score(score)
                .hedgeWordsFound(hedgeFound)
                .clickbaitIndicators(clickbaitFound)
                .emotionalWordsFound(emotionalFound)
                .manipulationIndicators(manipulationFound)
                .conspiracyIndicators(conspiracyFound)
                .build();
    }

    private static Pattern pattern(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    private String merge(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (p != null) sb.append(p).append(' ');
        return sb.toString();
    }
}

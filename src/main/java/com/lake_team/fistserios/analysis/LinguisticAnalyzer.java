package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LinguisticAnalyzer {

    private static final Set<String> HEDGE_EN = Set.of(
            "allegedly", "reportedly", "apparently", "seemingly", "supposedly",
            "claimed", "unconfirmed", "purportedly", "rumored", "speculated",
            "possibly", "perhaps", "maybe", "might", "uncertain", "unclear",
            "unverified", "believed", "thought", "suggested", "implied",
            "according to sources", "sources say", "anonymous sources",
            "said to be", "it is claimed", "it is believed"
    );
    private static final Set<String> HEDGE_UK = Set.of(
            "нібито", "начебто", "мовляв", "буцімто", "схоже", "можливо",
            "мабуть", "очевидно", "напевно", "здається", "стверджується",
            "повідомляється", "за чутками", "невідомо", "непідтверджено",
            "вважається", "так звані", "неперевірено"
    );
    private static final Set<String> HEDGE_WORDS = combined(HEDGE_EN, HEDGE_UK);

    private static final Set<String> CLICKBAIT_EN = Set.of(
            "shocking", "unbelievable", "mind-blowing", "jaw-dropping",
            "you won't believe", "secret", "revealed", "exposed",
            "breaking", "exclusive", "incredible", "viral", "trending",
            "the truth about", "what they don't want you to know",
            "this is why", "here's why", "find out",
            "must read", "wake up", "share this",
            "mainstream media won't", "watch before deleted",
            "they're hiding", "the real truth", "bombshell"
    );
    private static final Set<String> CLICKBAIT_UK = Set.of(
            "шокуючий", "шокуюче", "шокує", "неймовірний", "неймовірно",
            "вражаючий", "вражаюче", "не повіриш", "не повірите",
            "таємниця розкрита", "таємниця розкрито", "викрито",
            "вірусний", "вірусне", "сенсація", "ексклюзив", "сенсаційний",
            "терміново", "правда про", "ось чому", "тут чому",
            "обов'язково прочитай", "прокинься", "поділись цим"
    );
    private static final Set<String> CLICKBAIT_WORDS = combined(CLICKBAIT_EN, CLICKBAIT_UK);

    private static final Set<String> EMOTIONAL_EN = Set.of(
            "catastrophe", "catastrophic", "disaster", "devastating", "crisis",
            "chaos", "terror", "panic", "outrage", "fury", "rage",
            "scandal", "explosive", "horrifying", "alarming", "appalling",
            "atrocious", "dreadful", "disgusting", "vile", "monstrous",
            "propaganda", "manipulation", "censorship", "cover-up", "conspiracy",
            "corrupt", "corruption", "treasonous", "treason", "criminal",
            "witch hunt", "rigged", "stolen", "fraud", "hoax",
            "radical", "extreme", "extremist", "destroy", "destroying"
    );
    private static final Set<String> EMOTIONAL_UK = Set.of(
            "катастрофа", "катастрофічний", "хаос", "терор", "паніка",
            "обурення", "лють", "гнів", "скандал", "пропаганда",
            "маніпуляція", "цензура", "корупція", "зрада", "злочин",
            "злочинний", "огидний", "жахливий", "мерзотний", "фальсифікація",
            "змова", "шахрайство", "брехня", "руйнування", "знищення",
            "радикал", "екстремізм", "екстреміст"
    );
    private static final Set<String> EMOTIONAL_WORDS = combined(EMOTIONAL_EN, EMOTIONAL_UK);

    private static final List<Pattern> MANIPULATION_PATTERNS = List.of(
            // English
            pattern("(fake|lying|biased|corrupt) (news|media|press|journalist)"),
            pattern("BREAKING[:\\s].{0,60}(obama|clinton|trump|biden|democrat|republican)"),
            pattern("caught (red.handed|lying|cheating|stealing|committing)"),
            pattern("(everyone knows|all true (patriots|americans)|real (patriots|americans))"),
            pattern("(globalist|socialist|communist|marxist) (agenda|plot|takeover|scheme)"),
            pattern("will (destroy|ruin|end) (america|the country|our nation|democracy)"),
            // Ukrainian
            pattern("(фейков|брехлив|упередж).{0,15}(новин|зміях|медіа|журналіст|прес)"),
            pattern("впіймали.{0,10}(на гарячому|на брехні|за руку)"),
            pattern("(всі знають|всі справжні|реальні українці|справжні патріоти)"),
            pattern("(глобалістськ|соціалістичн|комуністичн|марксистськ).{0,15}(план|агенд|схем|змов)"),
            pattern("(знищить|зруйнує|покладе край|поховає).{0,20}(украін|країн|демократі|свободу)")
    );

    private static final List<Pattern> CONSPIRACY_PATTERNS = List.of(
            // English
            pattern("(is|might be|could be|still|actually) alive"),
            pattern("faked (his|her|their|the)? ?death"),
            pattern("never (really )?died|didn'?t (really )?die"),
            pattern("(secret(ly)?|actually) (replaced|cloned|a clone|a double)"),
            pattern("crisis actor|false flag"),
            pattern("illuminati|lizard (people|person)|flat ?earth|new world order"),
            pattern("chemtrail|microchip(ped)?|5g (causes|kills|spread)"),
            pattern("(they|government|media|cdc|who) (don'?t|doesn'?t) want you to (know|see|hear)"),
            pattern("(hidden|suppressed|banned|forbidden) (truth|cure|video|technology)"),
            pattern("government (is )?hiding|deep state (plot|agenda|conspiracy)"),
            // Ukrainian
            pattern("(підставн|кризов).{0,10}(актор|провокатор)"),
            pattern("фальшивий прапор|провокація.{0,10}(агент|служб)"),
            pattern("(глибока держава|тіньовий уряд|таємний уряд)"),
            pattern("хімтрейл|мікрочіп.{0,15}(вакцин|укол)|5g.{0,10}(викликає|вбиває|поширює)"),
            pattern("(вони|уряд|влада).{0,15}(приховують|замовчують).{0,10}(правду|факти)"),
            pattern("(прихован|заборонен|таємн).{0,10}(правда|лікування|відео|технологія)"),
            pattern("держава.{0,10}(приховує|замовчує)")
    );

    private static final List<Pattern> ANONYMOUS_SOURCE_PATTERNS = List.of(
            // English
            pattern("(many|some|several|most) people (say|claim|believe|think|argue)"),
            pattern("(insiders?|officials?|sources?|experts?) (say|claim|reveal|told us|suggest)"),
            pattern("(anonymous|unnamed|undisclosed) (source|official|insider|contact)"),
            pattern("(critics?|observers?) (argue|claim|say|note|warn)"),
            pattern("(it is|it's) (widely|generally|commonly) (believed|known|understood|said)"),
            pattern("(rumor|word) (has it|is|goes) (that|around)"),
            // Ukrainian
            pattern("(багато|деякі|більшість).{0,10}(людей|осіб|громадян).{0,15}(кажуть|вважають|стверджують)"),
            pattern("(інсайдери?|джерела?|чиновники?|офіційні особи).{0,15}(кажуть|стверджують|повідомляють|розповіли)"),
            pattern("(анонімн|неназван|нерозкрит).{0,10}(чиновник|інсайдер|джерело|особа)"),
            pattern("(критики|спостерігачі|аналітики).{0,15}(стверджують|вважають|кажуть|попереджають)"),
            pattern("(широко|загально|повсюдно).{0,10}(вважається|відомо|вважають)"),
            pattern("(за чутками|ходять чутки|чутки свідчать)")
    );

    private static final List<Pattern> CITATION_PATTERNS = List.of(
            // English
            pattern("(according to|said|stated|confirmed|told|reported by|cited by|per) [A-Z][a-z]+"),
            pattern("(said|stated|confirmed|announced|declared) (in|at|during) (a|an|the)"),
            pattern("\\\"[^\\\"]{10,}\\\" ?(said|wrote|stated|told|confirmed)"),
            pattern("(professor|doctor|dr\\.|prof\\.|spokesperson|minister|secretary) [A-Z][a-z]+"),
            pattern("(study|report|research|survey|analysis) (by|from|published in|in)"),
            pattern("(department|ministry|agency|bureau|commission|committee) (said|confirmed|stated)"),
            // Ukrainian
            pattern("(за словами|повідомив|заявив|підтвердив|розповів|зазначив|наголосив).{0,5}[А-ЯІЇЄ]"),
            pattern("[«\"][^«»\"]{10,}[»\"].{0,15}(заявив|повідомив|написав|підтвердив|стверджує)"),
            pattern("(дослідження|звіт|аналіз|опитування|доповідь).{0,10}(від|опубліковано|показало|свідчить)"),
            pattern("(міністерство|відомство|служба|агентство|комісія|комітет).{0,15}(повідомило|підтвердило|заявило)"),
            pattern("(академік|доктор|д-р|професор|проф\\.)\\s+[А-ЯІЇЄ]")
    );

    private static final Pattern BETTERIDGE = Pattern.compile(
            "^(is|are|was|were|will|would|could|can|did|does|do|has|have|should|might|may) .+\\?$" +
            "|^(чи є|чи були|чи буде|чи може|чи міг|чи могла|чи справді|чи дійсно" +
            "|чи стане|чи правда|чи загрожує|чи вдасться|чи варто) .+\\?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Set<String> STOP_WORDS = combined(
            Set.of("the","a","an","and","or","but","in","on","at","to","for","of",
                   "with","by","from","up","about","into","through","is","are","was",
                   "were","be","been","being","have","has","had","do","does","did",
                   "will","would","could","should","may","might","this","that","these",
                   "those","it","its","as","not","than","then","so","if","he","she",
                   "we","they","you","i","their","his","her","our","your","my"),
            Set.of("та","і","й","або","але","у","в","на","до","за","від","по","при",
                   "про","через","між","над","під","без","перед","після","це","той",
                   "ця","ці","він","вона","вони","ми","ви","я","його","її","їх",
                   "наш","ваш","мій","є","був","була","були","буде","бути","не","як",
                   "що","який","яка","яке","які","де","коли","тому","якщо","щоб")
    );

    private static final int MAX_SCORE                = 35;
    private static final int MAX_HEDGE_PENALTY        = 15;
    private static final int MAX_CLICKBAIT_PENALTY    = 10;
    private static final int MAX_EMOTIONAL_PENALTY    = 10;
    private static final int MAX_MANIPULATION_PENALTY = 16;
    private static final int CONSPIRACY_PENALTY       = 20;
    private static final int MAX_ANON_PENALTY         = 9;
    private static final int MAX_CITATION_PENALTY     = 6;
    private static final int MAX_HEADLINE_PENALTY     = 8;
    private static final int BETTERIDGE_PENALTY       = 4;


    public LinguisticResult analyze(NewsItem item) {
        return analyze(item, AnalysisOptions.LinguisticSubOptions.all());
    }

    public LinguisticResult analyze(NewsItem item, AnalysisOptions.LinguisticSubOptions sub) {
        String fullText  = merge(item.getTitle(), item.getDescription(), item.getFullContent()).toLowerCase();
        String titleOnly = item.getTitle() != null ? item.getTitle() : "";
        String titleLow  = titleOnly.toLowerCase();

        List<String> hedgeFound = sub.hedgeWords()
                ? HEDGE_WORDS.stream().filter(fullText::contains).collect(Collectors.toList())
                : List.of();

        List<String> clickbaitFound = new ArrayList<>();
        if (sub.clickbait()) {
            CLICKBAIT_WORDS.stream().filter(titleLow::contains).forEach(clickbaitFound::add);
            if (titleOnly.matches(".*[!?]{2,}.*"))        clickbaitFound.add("excessive punctuation");
            if (titleOnly.matches(".*\\b[A-Z]{4,}\\b.*")) clickbaitFound.add("ALL CAPS word");
        }

        List<String> emotionalFound = sub.emotional()
                ? EMOTIONAL_WORDS.stream().filter(fullText::contains).collect(Collectors.toList())
                : List.of();

        List<String> manipulationFound = sub.manipulation()
                ? MANIPULATION_PATTERNS.stream()
                        .filter(p -> p.matcher(fullText).find())
                        .map(Pattern::pattern)
                        .collect(Collectors.toList())
                : List.of();

        String titleAndDesc = merge(item.getTitle(), item.getDescription()).toLowerCase();
        List<String> conspiracyFound = sub.conspiracy()
                ? CONSPIRACY_PATTERNS.stream()
                        .filter(p -> p.matcher(titleAndDesc).find())
                        .map(Pattern::pattern)
                        .collect(Collectors.toList())
                : List.of();

        List<String> anonFound = sub.anonymousSources()
                ? ANONYMOUS_SOURCE_PATTERNS.stream()
                        .filter(p -> p.matcher(fullText).find())
                        .map(m -> findFirstMatch(m, fullText))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                : List.of();

        int citationCount = 0;
        if (sub.citationDensity()) {
            citationCount = (int) CITATION_PATTERNS.stream()
                    .filter(p -> p.matcher(merge(item.getTitle(), item.getDescription(), item.getFullContent())).find())
                    .count();
        }

        double headlineOverlap = 1.0;
        if (sub.headlineConsistency() && item.getTitle() != null) {
            headlineOverlap = computeHeadlineOverlap(
                    item.getTitle(), merge(item.getDescription(), item.getFullContent()));
        }

        boolean betteridge = sub.betteridge() && BETTERIDGE.matcher(titleOnly.trim()).matches();

        int penalty = 0;
        if (sub.hedgeWords())      penalty += Math.min(hedgeFound.size()        * 3, MAX_HEDGE_PENALTY);
        if (sub.clickbait())       penalty += Math.min(clickbaitFound.size()    * 5, MAX_CLICKBAIT_PENALTY);
        if (sub.emotional())       penalty += Math.min(emotionalFound.size()    * 2, MAX_EMOTIONAL_PENALTY);
        if (sub.manipulation())    penalty += Math.min(manipulationFound.size() * 8, MAX_MANIPULATION_PENALTY);
        if (sub.conspiracy() && !conspiracyFound.isEmpty()) penalty += CONSPIRACY_PENALTY;
        if (sub.anonymousSources())penalty += Math.min(anonFound.size()         * 3, MAX_ANON_PENALTY);

        if (sub.citationDensity()) {
            if (citationCount == 0)      penalty += MAX_CITATION_PENALTY;
            else if (citationCount <= 2) penalty += MAX_CITATION_PENALTY / 2;
        }

        if (sub.headlineConsistency()) {
            if      (headlineOverlap < 0.20) penalty += MAX_HEADLINE_PENALTY;
            else if (headlineOverlap < 0.40) penalty += MAX_HEADLINE_PENALTY / 2;
        }

        if (betteridge) penalty += BETTERIDGE_PENALTY;

        int score = Math.max(MAX_SCORE - penalty, 0);

        return LinguisticResult.builder()
                .score(score)
                .hedgeWordsFound(hedgeFound)
                .clickbaitIndicators(clickbaitFound)
                .emotionalWordsFound(emotionalFound)
                .manipulationIndicators(manipulationFound)
                .conspiracyIndicators(conspiracyFound)
                .anonymousSourcesFound(anonFound)
                .citationCount(citationCount)
                .headlineBodyOverlap(headlineOverlap)
                .betteridgeDetected(betteridge)
                .build();
    }

    private double computeHeadlineOverlap(String title, String body) {
        if (body == null || body.isBlank()) return 1.0;
        Set<String> titleWords = tokenize(title);
        Set<String> bodyWords  = tokenize(body);
        if (titleWords.isEmpty()) return 1.0;
        long overlap = titleWords.stream().filter(bodyWords::contains).count();
        return (double) overlap / titleWords.size();
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        return Arrays.stream(text.toLowerCase().split("[^a-zа-яіїєґ0-9]+"))
                .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }

    private String findFirstMatch(Pattern p, String text) {
        var m = p.matcher(text);
        if (!m.find()) return null;
        String match = m.group();
        return match.length() > 60 ? match.substring(0, 60) + "…" : match;
    }

    private static Pattern pattern(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private String merge(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (p != null) sb.append(p).append(' ');
        return sb.toString();
    }

    @SafeVarargs
    private static <T> Set<T> combined(Set<T>... sets) {
        Set<T> result = new HashSet<>();
        for (Set<T> s : sets) result.addAll(s);
        return Collections.unmodifiableSet(result);
    }
}

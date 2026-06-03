/**
 * method-info-data.js
 * Shared methodology info panels — used in modal (main.html) and article sidebar (article.html).
 *
 * Each entry: { title, badge, desc, formula?, examples?, details? }
 * String value → alias to another key.
 */

const MINFO = {

    /* ══════════════════════════════════════════
       LAYER HEADERS — Modal
    ══════════════════════════════════════════ */

    optLinguistic: {
        title: 'Лінгвістичний аналіз',
        badge: '0 – 35 балів',
        desc: 'Аналізує мову статті без зовнішніх API-запитів. Базовий бал шару — 35. Кожен виявлений патерн знімає певну кількість балів.',
        formula: 'score = max(35 − Σ штрафів, 0)',
        details: [
            '9 незалежних підметодів — кожен можна вмикати / вимикати окремо.',
            'Штрафи накопичуються незалежно. Конспірологічні патерни додатково обмежують підсумковий бал статті до 40.',
            'Якщо всі підметоди вимкнено — шар повертає 0 балів.',
        ],
    },

    optCrossSource: {
        title: 'Крос-перевірка джерел',
        badge: '0 – 35 балів',
        desc: 'Витягує ключові слова з заголовку (top-3 за довжиною) і паралельно шукає їх у трьох зовнішніх новинних API.',
        formula: '0 підтверджень → 0 · 1 → 10 · 2 → 22 · 3 → 35',
        details: [
            'Джерела: NewsAPI, The Guardian API, GNews.',
            'Автоматично вимикається для статей з датасету (sourceType = DATASET).',
            'Якщо виявлено конспірологічні патерни — бал цього шару обнуляється незалежно від знайдених збігів.',
        ],
    },

    optFactCheck: {
        title: 'Факт-чекінг',
        badge: '0 – 30 балів',
        desc: '5 незалежних підметодів, результати яких підсумовуються. Два з них (URL-аналіз та перевидана новина) можуть лише знімати бали — без позитивного внеску.',
        formula: 'score = max(rep + cb + rss + urlΔ + recycledΔ, 0)',
        details: [
            'Якщо всі підметоди вимкнено — шар повертає 0 балів.',
            'URL-аналіз та recycled-news мають від\'ємні або нульові штрафи.',
        ],
    },

    /* ══════════════════════════════════════════
       LINGUISTIC SUB-METHODS
    ══════════════════════════════════════════ */

    subHedge: {
        title: 'Хедж-слова',
        badge: 'штраф: −3 × N, max −15',
        desc: 'Хеджування — вживання слів невизначеності, що знімають відповідальність за достовірність. Частий маркер чуток і непідтвердженої інформації.',
        formula: 'штраф = −3 × кількість знайдених слів, max −15',
        examples: ['allegedly', 'reportedly', 'supposedly', 'purportedly', 'claimed', 'unconfirmed', 'rumored', 'possibly', 'perhaps', 'unclear', 'according to sources', 'said to be'],
        details: [
            'Перевіряється весь текст (заголовок + опис + повний зміст), регістр ігнорується.',
            '~25 слів і фраз у словнику.',
        ],
    },

    subClickbait: {
        title: 'Клікбейт',
        badge: 'штраф: −5 × N, max −10',
        desc: 'Сенсаційні слова та конструкції, покликані залучити кліки, а не передати інформацію.',
        formula: 'штраф = −5 × кількість індикаторів, max −10',
        examples: ['shocking', 'mind-blowing', 'you won\'t believe', 'secret', 'exposed', 'bombshell', 'viral', 'the real truth', 'must read', 'ALL CAPS word', '!? ×2'],
        details: [
            'Перевіряється лише заголовок статті (clickbait у тілі не рахується).',
            'Додаткові ознаки: 2+ підряд знаки пунктуації (!!, ??) та слово з 4+ великих літер (BREAKING, WATCH).',
            '~25 фраз у словнику.',
        ],
    },

    subEmotional: {
        title: 'Емоційна лексика',
        badge: 'штраф: −2 × N, max −10',
        desc: 'Слова із сильним негативним емоційним забарвленням, що нагнітають страх чи обурення без аналітики.',
        formula: 'штраф = −2 × кількість знайдених слів, max −10',
        examples: ['catastrophe', 'chaos', 'terror', 'outrage', 'scandal', 'propaganda', 'rigged', 'fraud', 'hoax', 'witch hunt', 'monstrous', 'conspiracy', 'treason'],
        details: [
            'Перевіряється весь текст.',
            '~40 слів у словнику.',
        ],
    },

    subManipulation: {
        title: 'Маніпулятивні патерни',
        badge: 'штраф: −8 × N, max −16',
        desc: 'Regex-патерни маніпулятивних наративів — зазвичай політичного характеру.',
        formula: 'штраф = −8 × кількість знайдених патернів, max −16',
        examples: [
            '"fake/lying/biased news/media"',
            '"caught red-handed/lying/cheating"',
            '"everyone knows / all true patriots"',
            '"globalist/socialist/communist agenda"',
            '"will destroy America / our democracy"',
            '"BREAKING: ... Obama/Clinton/Biden"',
        ],
        details: [
            'Перевіряється весь текст (6 регекс-патернів), регістр ігнорується.',
        ],
    },

    subConspiracy: {
        title: 'Конспірологія',
        badge: 'штраф: −20 + cap 40',
        desc: 'Виявляє конспірологічні твердження. При спрацюванні накладається фіксований штраф −20 і загальний score статті обмежується значенням 40.',
        formula: 'штраф = −20 (фіксований), загальний credibilityScore ≤ 40',
        examples: [
            'false flag / crisis actor',
            'illuminati / lizard people / flat earth',
            'deep state plot / government hiding',
            'chemtrail / microchip / 5G causes',
            'faked death / never really died',
            'hidden/suppressed/banned truth',
        ],
        details: [
            'Перевіряється заголовок + опис (без повного тексту, щоб уникнути хибних спрацювань на аналітичні статті про конспірології).',
            'Cap 40 застосовується до підсумкового credibilityScore навіть якщо інші шари дали хороші результати.',
            '10 регекс-патернів у базі.',
        ],
    },

    subAnonSources: {
        title: 'Анонімна атрибуція',
        badge: 'штраф: −3 × N, max −9',
        desc: 'Посилання на анонімних або узагальнених «інсайдерів» без конкретних імен — ознака слабкої журналістики або дезінформації.',
        formula: 'штраф = −3 × кількість патернів, max −9',
        examples: [
            '"many/some people say/believe"',
            '"sources reveal / insiders told us"',
            '"anonymous official / unnamed source"',
            '"critics argue / observers warn"',
            '"it is widely believed"',
            '"rumor has it"',
        ],
        details: [
            'Перевіряється весь текст (6 регекс-патернів).',
        ],
    },

    subCitationDensity: {
        title: 'Щільність цитат',
        badge: '0 → −6 · 1–2 → −3 · 3+ → 0',
        desc: 'Підраховує чіткі атрибуції — посилання на конкретних осіб, організації, дослідження. Позитивний сигнал надійності.',
        formula: '0 цитат → −6 · 1–2 → −3 · 3+ → 0 (без штрафу)',
        examples: [
            '"according to Reuters"',
            '"Dr. Smith stated"',
            '"confirmed at the press conference"',
            '"study published in Nature"',
            '"Ministry confirmed"',
            '"Prof. Jones wrote"',
        ],
        details: [
            'Перевіряється весь текст (6 регекс-патернів атрибуції).',
            'Наявність 3+ чітких цитат — хороший сигнал; штрафу немає.',
        ],
    },

    subHeadlineConsistency: {
        title: 'Узгодженість заголовку',
        badge: '<20% → −8 · 20–40% → −4',
        desc: 'Порівнює ключові слова заголовку зі словами тіла статті. Низький збіг — ознака «жовтих» заголовків, що не відповідають змісту.',
        formula: 'overlap = |titleWords ∩ bodyWords| / |titleWords|\n<20% → −8 · 20–40% → −4 · ≥40% → 0',
        details: [
            'Стоп-слова (the, is, and…) та слова ≤ 2 символи виключаються.',
            'Якщо тіло статті відсутнє — штраф не накладається (overlap = 1.0).',
            'Типовий приклад: "SHOCKING: You won\'t believe what happened!" при статті про ціни на бензин.',
        ],
    },

    subBetteridge: {
        title: 'Закон Беттерідж',
        badge: 'штраф: −4',
        desc: 'Закон Беттерідж (Ian Betteridge, 1990): «На будь-яку статтю з заголовком у формі питання можна відповісти словом "Ні".» Клікбейт-видання часто використовують цей прийом.',
        formula: 'заголовок починається з Is/Are/Was/Will/Could/Can/Did… та закінчується на ? → −4',
        examples: [
            '"Is the government hiding the truth?"',
            '"Are vaccines causing autism?"',
            '"Will this destroy our economy?"',
            '"Did they really fake the Moon landing?"',
        ],
        details: [
            'Перевіряється лише перший токен заголовку; заголовок повинен закінчуватися на «?».',
        ],
    },

    /* ══════════════════════════════════════════
       FACT-CHECK SUB-METHODS
    ══════════════════════════════════════════ */

    subSourceRep: {
        title: 'Репутація домену',
        badge: '0 – 12 балів',
        desc: 'Оцінює надійність домену-джерела на основі бази відомих медіа. Домени .gov та .edu отримують максимальний бал автоматично.',
        formula: 'HIGH → 12 · GOOD → 9 · UNKNOWN → 5 · LOW → 0',
        details: [
            'HIGH (12): reuters.com, apnews.com, bbc.com, npr.org, who.int, nature.com, science.org, ft.com, wsj.com, economist.com…',
            'GOOD (9): theguardian.com, nytimes.com, bloomberg.com, politico.com, dw.com, france24.com, euronews.com…',
            'LOW (0): infowars.com, naturalnews.com, worldnewsdailyreport.com, theonion.com, babylonbee.com…',
            'UNKNOWN (5): домени, яких немає в жодному списку.',
        ],
    },

    subExtendedUrl: {
        title: 'Розширений URL-аналіз',
        badge: 'штраф: 0 / −2 / −4 / −5',
        desc: 'Аналізує структуру URL на ознаки підроблених або ненадійних доменів.',
        formula: 'CLEAN → 0 · SUSPICIOUS (1 ознака) → −2 · SUSPICIOUS (2) → −4 · FAKE (3+) → −5',
        examples: [
            'Підозрілі TLD: .xyz · .click · .info · .top · .tk · .ml · .ga · .cf',
            'Мімікрія бренду: cnn-news.xyz · abcnews.com.co · bbc-today.info',
            'Надмірні субдомени: real.news.today.site.com (3+ рівні)',
            'IP-адреса як хост: http://192.168.1.1/article',
            'Надто довге ім\'я домену (>30 символів)',
        ],
    },

    subClaimBuster: {
        title: 'ClaimBuster (евристика)',
        badge: '0 – 10 балів',
        desc: 'Оцінює «перевірабельність» тверджень — наскільки стаття містить факти, що піддаються верифікації. Власна евристика (не зовнішнє API).',
        formula: '+2pts за кожну категорію, що виявлена (5 категорій, max 10)',
        details: [
            '+2: числові факти — відсотки, суми, дати ("GDP grew by 3.2%")',
            '+2: абсолютні квантори — "never", "always", "every single"',
            '+2: дієслова заяв — "said", "claimed", "announced", "confirmed"',
            '+2: порівняльні структури — "more than", "less than", "higher than"',
            '+2: умовні речення — "if … then", "would cause"',
        ],
    },

    subRss: {
        title: 'RSS-перевірка',
        badge: '0 – 8 балів',
        desc: 'Порівнює заголовок статті з останніми матеріалами Snopes і PolitiFact через їх RSS-стрічки.',
        formula: 'TRUE/MOSTLY TRUE → 8 · HALF TRUE → 4 · FALSE → 0 · не знайдено → 6',
        details: [
            'Джерела: Snopes RSS, PolitiFact RSS.',
            'Порівняння: заголовки знаходяться за наявністю слова в рядку (toLowerCase contains).',
            '«Не знайдено» (6pts) — нейтральна оцінка, не штраф: відсутність у fact-check базах ≠ хибність.',
        ],
    },

    subRecycledNews: {
        title: 'Перевидана новина',
        badge: 'штраф: −5 при виявленні',
        desc: 'Виявляє старі новини, що видаються за свіжі — поширена техніка дезінформації та маніпуляції.',
        formula: 'виявлено → −5 (фінальний factCheckScore не нижче 0)',
        details: [
            'Ознака 1: рік у шляху URL (/2019/03/) відрізняється від дати публікації більш ніж на 1 рік.',
            'Ознака 2: 2+ явні дати в тексті (January 2018, 2017-05-12) старші за дату публікації >1р.',
            'Ознака 3: рік у заголовку значно старший за рік публікації.',
        ],
    },

    /* ══════════════════════════════════════════
       ADDITIONAL METRICS
    ══════════════════════════════════════════ */

    optSentiment: {
        title: 'Аналіз тональності',
        badge: 'не впливає на score',
        desc: 'Оцінює емоційне забарвлення тексту на основі словника AFINN-111 (~2 500 слів з оцінками від −5 до +5).',
        formula: 'score = Σ(оцінок слів) / кількість_знайдених_слів\nPOSITIVE: >0.1 · NEGATIVE: <−0.1 · NEUTRAL: решта',
        details: [
            'Приклади оцінок: "good" = +3, "excellent" = +3, "bad" = −3, "terrible" = −3, "love" = +3.',
            'Дуже емоційний текст не обов\'язково є неправдивим — це лише інформаційна метрика.',
        ],
    },

    optReadability: {
        title: 'Читабельність (Flesch)',
        badge: 'не впливає на score',
        desc: 'Flesch Reading Ease — стандартна формула оцінки складності тексту. Чим вище значення, тим легше читати.',
        formula: '206.835 − 1.015 × (слів/речень) − 84.6 × (складів/слів)',
        details: [
            '90–100: Дуже легко (рівень початкової школи)',
            '70–90: Легко',
            '60–70: Стандарт (12–13 років)',
            '50–60: Досить складно',
            '30–50: Складно (університетський рівень)',
            '0–30: Дуже складно (науковий або юридичний текст)',
        ],
    },

    optWikipedia: {
        title: 'Wikipedia-верифікація',
        badge: 'не впливає на score',
        desc: 'Витягує власні назви з тексту та перевіряє їх наявність у Wikipedia — додаткова ознака опори на реальні сутності.',
        formula: 'NER regex → Wikipedia REST API /api/rest_v1/page/summary/{entity}',
        details: [
            'NER-евристика: пари/трійки слів з великої літери (не на початку речення, не стоп-слова, >3 символи).',
            'До 8 сутностей на статтю (обмеження для запобігання API-спаму).',
            'Затримка 200 мс між запитами.',
            'Результат показується у вкладці «Факт-чек» лише для відображення.',
        ],
    },

    /* ══════════════════════════════════════════
       ARTICLE PANEL — layer header aliases
    ══════════════════════════════════════════ */
    artOptLinguistic:          'optLinguistic',
    artOptCrossSource:         'optCrossSource',
    artOptFactCheck:           'optFactCheck',

    /* Article panel — sub-method aliases */
    artSubHedge:               'subHedge',
    artSubClickbait:           'subClickbait',
    artSubEmotional:           'subEmotional',
    artSubManipulation:        'subManipulation',
    artSubConspiracy:          'subConspiracy',
    artSubAnonSources:         'subAnonSources',
    artSubCitationDensity:     'subCitationDensity',
    artSubHeadlineConsistency: 'subHeadlineConsistency',
    artSubBetteridge:          'subBetteridge',
    artSubSourceRep:           'subSourceRep',
    artSubExtendedUrl:         'subExtendedUrl',
    artSubClaimBuster:         'subClaimBuster',
    artSubRss:                 'subRss',
    artSubRecycledNews:        'subRecycledNews',
    artOptSentiment:           'optSentiment',
    artOptReadability:         'optReadability',
    artOptWikipedia:           'optWikipedia',
};

/* ══════════════════════════════════════════
   Toggle logic
══════════════════════════════════════════ */

function toggleMInfo(key, btn, event) {
    event.preventDefault();
    event.stopPropagation();

    // Resolve alias
    let info = MINFO[key];
    if (typeof info === 'string') info = MINFO[info];
    if (!info) return;

    const panel = document.getElementById('minfop-' + key);
    if (!panel) return;

    const isOpen = panel.classList.contains('minfo-open');

    // Close all open panels
    document.querySelectorAll('.minfo-panel.minfo-open').forEach(p => {
        p.classList.remove('minfo-open');
        p.innerHTML = '';
    });
    document.querySelectorAll('.minfo-btn.minfo-btn--active').forEach(b => {
        b.classList.remove('minfo-btn--active');
    });

    if (!isOpen) {
        panel.innerHTML = _buildMInfoHTML(info);
        panel.classList.add('minfo-open');
        btn.classList.add('minfo-btn--active');
        // Smooth scroll into view if needed
        setTimeout(() => panel.scrollIntoView({ block: 'nearest', behavior: 'smooth' }), 60);
    }
}

// Expose globally for inline onclick handlers
window.toggleMInfo = toggleMInfo;

function _buildMInfoHTML(info) {
    const esc = s => String(s ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    const nl2br = s => esc(s).replace(/\n/g, '<br>');

    let h = `<div class="minfo-inner">`;

    h += `<div class="minfo-head">
              <span class="minfo-head__title">${esc(info.title)}</span>
              <span class="minfo-head__badge">${esc(info.badge || '')}</span>
          </div>`;

    if (info.desc) {
        h += `<p class="minfo-desc">${esc(info.desc)}</p>`;
    }

    if (info.formula) {
        h += `<div class="minfo-formula-wrap">
                  <span class="minfo-label">Формула</span>
                  <code class="minfo-formula">${nl2br(info.formula)}</code>
              </div>`;
    }

    if (info.examples?.length) {
        h += `<div class="minfo-examples-wrap">
                  <span class="minfo-label">Приклади</span>
                  <div class="minfo-tags">
                      ${info.examples.map(e => `<span class="minfo-tag">${esc(e)}</span>`).join('')}
                  </div>
              </div>`;
    }

    if (info.details?.length) {
        h += `<ul class="minfo-details">
                  ${info.details.map(d => `<li>${esc(d)}</li>`).join('')}
              </ul>`;
    }

    h += `</div>`;
    return h;
}

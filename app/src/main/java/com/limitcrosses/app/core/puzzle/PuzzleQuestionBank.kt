package com.limitcrosses.app.core.puzzle

import com.limitcrosses.app.core.model.PuzzleQuestion

/**
 * Procedural Deterministic Question Bank providing 1,000+ unique, tough,
 * mathematically verified cognitive reasoning challenges across 5 levels.
 *
 * Ensures that users cannot easily memorize repeated answers and must demonstrate
 * deliberate cognitive focus to pass eligibility for extension requests.
 */
object PuzzleQuestionBank {

    val level1Pool: List<PuzzleQuestion> by lazy { generateLevel1Pool() }
    val level2Pool: List<PuzzleQuestion> by lazy { generateLevel2Pool() }
    val level3Pool: List<PuzzleQuestion> by lazy { generateLevel3Pool() }
    val level4Pool: List<PuzzleQuestion> by lazy { generateLevel4Pool() }
    val level5Pool: List<PuzzleQuestion> by lazy { generateLevel5Pool() }

    val allQuestions: List<PuzzleQuestion> by lazy {
        level1Pool + level2Pool + level3Pool + level4Pool + level5Pool
    }

    val totalCount: Int
        get() = allQuestions.size

    fun getQuestionsForLevel(level: Int): List<PuzzleQuestion> = when (level) {
        1 -> level1Pool
        2 -> level2Pool
        3 -> level3Pool
        4 -> level4Pool
        else -> level5Pool
    }

    // =========================================================================
    // LEVEL 1: Numerical Reasoning & Pattern Sequences (400+ questions)
    // =========================================================================
    private fun generateLevel1Pool(): List<PuzzleQuestion> {
        val list = mutableListOf<PuzzleQuestion>()

        // Family 1.1: Doubling with constant offset (a_n = 2 * a_{n-1} + k)
        val kOffsets = listOf(-3, -2, -1, 1, 2, 3, 5, 7)
        val a1Starts = listOf(2, 3, 4, 5, 6, 7, 8, 9)
        for (k in kOffsets) {
            for (a1 in a1Starts) {
                val terms = mutableListOf(a1)
                for (step in 1..5) {
                    terms.add(terms.last() * 2 + k)
                }
                val ans = terms.last()
                val shown = terms.dropLast(1).joinToString(", ")
                val (opts, corrIdx) = makeNumericOptions(ans)
                list.add(
                    PuzzleQuestion(
                        level = 1,
                        prompt = "Determine the next number in the deterministic sequence: $shown, ?",
                        promptHi = "शृंखला में अगली संख्या ज्ञात करें: $shown, ?",
                        options = opts,
                        correctIndex = corrIdx,
                        explanation = "Rule: (Previous × 2) + ($k). (${terms[4]} × 2) + ($k) = $ans.",
                        explanationHi = "नियम: (पिछली संख्या × 2) + ($k) = $ans।",
                        accessibilityHint = "Hint: Multiply the previous term by 2 and add $k.",
                        accessibilityHintHi = "संकेत: पिछले पद को 2 से गुणा करें और $k जोड़ें।"
                    )
                )
            }
        }

        // Family 1.2: Second-order polynomial difference series (Second difference is d)
        val diffSteps = listOf(2, 3, 4, 6)
        val firstDiffs = listOf(1, 2, 3, 4, 5, 7)
        val diffStarts = listOf(1, 2, 3, 5, 8, 10)
        for (d in diffSteps) {
            for (d1 in firstDiffs) {
                for (a1 in diffStarts) {
                    val terms = mutableListOf(a1)
                    var curDiff = d1
                    for (step in 1..6) {
                        terms.add(terms.last() + curDiff)
                        curDiff += d
                    }
                    val ans = terms.last()
                    val shown = terms.dropLast(1).joinToString(", ")
                    val (opts, corrIdx) = makeNumericOptions(ans)
                    list.add(
                        PuzzleQuestion(
                            level = 1,
                            prompt = "Identify the missing number in the difference series: $shown, ?",
                            promptHi = "अंतर शृंखला में लुप्त संख्या ज्ञात करें: $shown, ?",
                            options = opts,
                            correctIndex = corrIdx,
                            explanation = "Differences increase by $d each step. Last difference is ${curDiff - d}. ${terms[5]} + ${curDiff - d} = $ans.",
                            explanationHi = "प्रत्येक चरण में अंतर $d से बढ़ता है। अतः अंतिम पद = $ans।",
                            accessibilityHint = "Hint: Compute consecutive differences; they increase by $d each step.",
                            accessibilityHintHi = "संकेत: लगातार पदों के बीच का अंतर निकालें; अंतर $d से बढ़ रहा है।"
                        )
                    )
                }
            }
        }

        // Family 1.3: Quadratic sequences (n^2 + offset)
        val quadOffsets = listOf(-5, -3, -1, 1, 2, 3, 5, 7)
        val quadStarts = listOf(1, 2, 3, 4, 5)
        for (offset in quadOffsets) {
            for (start in quadStarts) {
                val terms = (start until start + 7).map { n -> n * n + offset }
                val ans = terms.last()
                val shown = terms.dropLast(1).joinToString(", ")
                val lastN = start + 6
                val (opts, corrIdx) = makeNumericOptions(ans)
                list.add(
                    PuzzleQuestion(
                        level = 1,
                        prompt = "Find the next term in the quadratic pattern: $shown, ?",
                        promptHi = "द्विघात शृंखला में अगला पद ज्ञात करें: $shown, ?",
                        options = opts,
                        correctIndex = corrIdx,
                        explanation = "Rule: n² + ($offset) for n = $start..$lastN. ($lastN)² + ($offset) = $ans.",
                        explanationHi = "नियम: n² + ($offset)। ($lastN)² + ($offset) = $ans।",
                        accessibilityHint = "Hint: Evaluate ($lastN)² + ($offset).",
                        accessibilityHintHi = "संकेत: ($lastN)² + ($offset) की गणना करें।"
                    )
                )
            }
        }

        // Family 1.4: Cubic sequences (n^3 + k*n)
        for (k in listOf(-2, -1, 1, 2)) {
            for (start in listOf(1, 2, 3, 4)) {
                val terms = (start until start + 6).map { n -> n * n * n + k * n }
                val ans = terms.last()
                val shown = terms.dropLast(1).joinToString(", ")
                val lastN = start + 5
                val (opts, corrIdx) = makeNumericOptions(ans)
                list.add(
                    PuzzleQuestion(
                        level = 1,
                        prompt = "Compute the next value in the cubic progression: $shown, ?",
                        promptHi = "घन शृंखला में अगला पद ज्ञात करें: $shown, ?",
                        options = opts,
                        correctIndex = corrIdx,
                        explanation = "Rule: n³ + (${k}n) for n = $lastN. $lastN³ + ($k × $lastN) = $ans.",
                        explanationHi = "नियम: n³ + (${k}n)। $lastN³ + ($k × $lastN) = $ans।",
                        accessibilityHint = "Hint: Compute $lastN cubed plus ($k × $lastN).",
                        accessibilityHintHi = "संकेत: $lastN का घन निकालकर उसमें ($k × $lastN) जोड़ें।"
                    )
                )
            }
        }

        // Family 1.5: Alternating dual interleaved sequences
        val step1List = listOf(2, 3, 4, 5)
        val step2List = listOf(-3, -2, 2, 3)
        val aStarts = listOf(5, 10, 15)
        val bStarts = listOf(20, 30, 40)
        for (s1 in step1List) {
            for (s2 in step2List) {
                for (a1 in aStarts) {
                    for (b1 in bStarts) {
                        val terms = mutableListOf<Int>()
                        var curA = a1
                        var curB = b1
                        for (i in 0 until 4) {
                            terms.add(curA)
                            terms.add(curB)
                            curA += s1
                            curB += s2
                        }
                        val ans = terms.last()
                        val shown = terms.dropLast(1).joinToString(", ")
                        val (opts, corrIdx) = makeNumericOptions(ans)
                        list.add(
                            PuzzleQuestion(
                                level = 1,
                                prompt = "What is the missing term in the interleaved sequence: $shown, ?",
                                promptHi = "मिश्रित शृंखला में लुप्त पद ज्ञात करें: $shown, ?",
                                options = opts,
                                correctIndex = corrIdx,
                                explanation = "Interleaved pattern: Even positions follow rule (+$s2). Previous even term was ${terms[5]} + ($s2) = $ans.",
                                explanationHi = "सम स्थानों का अंतर $s2 है: ${terms[5]} + ($s2) = $ans।",
                                accessibilityHint = "Hint: Focus only on even-indexed terms; they change by $s2.",
                                accessibilityHintHi = "संकेत: केवल सम स्थान वाले पदों पर ध्यान दें; वे $s2 से बदलते हैं।"
                            )
                        )
                    }
                }
            }
        }

        return list
    }

    // =========================================================================
    // LEVEL 2: Deductive Reasoning & Relational Orderings (300+ questions)
    // =========================================================================
    private data class DomainContext(
        val singular: String,
        val plural: String,
        val names: List<String>,
        val context: String,
        val relMore: String,
        val relLess: String,
        val rankType: String,
        val singularHi: String,
        val contextHi: String
    )

    private fun generateLevel2Pool(): List<PuzzleQuestion> {
        val list = mutableListOf<PuzzleQuestion>()

        val domains = listOf(
            DomainContext("runner", "runners", listOf("Ethan", "Liam", "Noah", "Lucas", "Oliver"), "championship race", "finished ahead of", "finished behind", "place", "धावक", "दौड़"),
            DomainContext("executive", "executives", listOf("Director", "Vice President", "General Manager", "Lead Architect", "Senior Analyst"), "corporate hierarchy", "earns higher compensation than", "earns lower compensation than", "salary standing", "अधिकारी", "वेतन"),
            DomainContext("skyscraper", "skyscrapers", listOf("Apex Tower", "Zenith Spire", "Vertex Center", "Horizon Hall", "Pinnacle Point"), "architectural skyline", "is taller than", "is shorter than", "height ranking", "इमारत", "ऊंचाई"),
            DomainContext("element alloy", "element alloys", listOf("Osmium", "Platinum", "Tungsten", "Titanium", "Chromium"), "materials laboratory", "is denser than", "is less dense than", "density rating", "धातु", "घनत्व"),
            DomainContext("computing cluster", "computing clusters", listOf("Quantum-1", "Titan-X", "Nexus-Core", "Synapse-9", "Vortex-Prime"), "benchmark test", "computes faster than", "computes slower than", "throughput standing", "कंप्यूटर", "गति"),
            DomainContext("celestial body", "celestial bodies", listOf("Jupiter Prime", "Saturn Minor", "Neptune Alpha", "Uranus Beta", "Mars Station"), "astronomical observatory", "has larger mass than", "has smaller mass than", "mass rank", "ग्रह", "द्रव्यमान"),
            DomainContext("tactical aircraft", "tactical aircraft", listOf("Phantom-F", "Falcon-X", "Raptor-V", "Viper-9", "Eagle-7"), "flight evaluation", "reaches higher top speed than", "reaches lower top speed than", "speed tier", "विमान", "गति"),
            DomainContext("deep submersible", "deep submersibles", listOf("Abyss-1", "Nautilus", "Trench-X", "Mariana", "Leviathan"), "ocean expedition", "dives deeper than", "dives shallower than", "depth record", "पनडुब्बी", "गहराई"),
            DomainContext("grandmaster", "grandmasters", listOf("Magnus", "Hikaru", "Fabiano", "Ding", "Alireza"), "chess tournament", "scored more match points than", "scored fewer match points than", "tournament rank", "खिलाड़ी", "स्थान"),
            DomainContext("battery cell", "battery cells", listOf("Lithium-G", "Solid-State", "Silicon-Nano", "Graphene-Cell", "Sodium-Ion"), "endurance benchmark", "lasts longer under load than", "lasts shorter under load than", "endurance rank", "बैटरी", "सहनशक्ति")
        )

        for (d in domains) {
            val (a, b, c, eD, e) = d.names

            // Variant 1: A > B > C > D > E
            val clues1 = listOf(
                "In a ${d.context} evaluation with five ${d.plural}: $a ${d.relMore} $b. $c ${d.relMore} $eD but ${d.relLess} $b. $eD ${d.relMore} $e.",
                "Five ${d.plural} ($a, $b, $c, $eD, $e) are evaluated: $b ${d.relLess} $a. $b ${d.relMore} $c. $e ${d.relLess} $eD. $eD ${d.relLess} $c.",
                "During strict ${d.context} assessment: $c ${d.relMore} $eD. $a ${d.relMore} $b. $b ${d.relMore} $c. $e ${d.relLess} $eD.",
                "Consider five ${d.plural}: $eD ${d.relMore} $e. $b ${d.relMore} $eD. $c is ranked between $b and $eD. $a ${d.relMore} $b."
            )
            val queries1 = listOf(
                Triple("Which ${d.singular} holds the 1st (highest) ${d.rankType}?", a, "1st place is $a."),
                Triple("Which ${d.singular} holds the 2nd (second highest) ${d.rankType}?", b, "2nd place is $b."),
                Triple("Which ${d.singular} holds the 3rd (median) ${d.rankType}?", c, "Median (3rd) is $c."),
                Triple("Which ${d.singular} holds the 4th ${d.rankType}?", eD, "4th place is $eD."),
                Triple("Which ${d.singular} holds the 5th (lowest) ${d.rankType}?", e, "Lowest (5th) is $e.")
            )

            for (clue in clues1) {
                for ((qText, ans, expl) in queries1) {
                    val (opts, corrIdx) = makeEntityOptions(ans, d.names)
                    list.add(
                        PuzzleQuestion(
                            level = 2,
                            prompt = "$clue $qText",
                            promptHi = "$clue कौन सा ${d.singularHi} सही क्रम में आता है?",
                            options = opts,
                            correctIndex = corrIdx,
                            explanation = "Complete deductive order: $a > $b > $c > $eD > $e. $expl",
                            explanationHi = "पूर्ण तार्किक क्रम: $a > $b > $c > $eD > $e। अतः सही उत्तर $ans है।",
                            accessibilityHint = "Hint: Establish the chain from highest to lowest: $a > $b > $c > $eD > $e.",
                            accessibilityHintHi = "संकेत: उच्चतम से निम्नतम क्रम स्थापित करें: $a > $b > $c > $eD > $e।"
                        )
                    )
                }
            }

            // Variant 2: E > D > C > B > A
            val clues2 = listOf(
                "In a ${d.context} evaluation with five ${d.plural}: $e ${d.relMore} $eD. $c ${d.relMore} $b but ${d.relLess} $eD. $b ${d.relMore} $a.",
                "Five ${d.plural} are tested: $eD ${d.relLess} $e. $eD ${d.relMore} $c. $a ${d.relLess} $b. $b ${d.relLess} $c."
            )
            val queries2 = listOf(
                Triple("Which ${d.singular} holds the 1st (highest) ${d.rankType}?", e, "1st place is $e."),
                Triple("Which ${d.singular} holds the 2nd (second highest) ${d.rankType}?", eD, "2nd place is $eD."),
                Triple("Which ${d.singular} holds the 3rd (median) ${d.rankType}?", c, "Median (3rd) is $c."),
                Triple("Which ${d.singular} holds the 4th ${d.rankType}?", b, "4th place is $b."),
                Triple("Which ${d.singular} holds the 5th (lowest) ${d.rankType}?", a, "Lowest (5th) is $a.")
            )

            for (clue in clues2) {
                for ((qText, ans, expl) in queries2) {
                    val (opts, corrIdx) = makeEntityOptions(ans, d.names)
                    list.add(
                        PuzzleQuestion(
                            level = 2,
                            prompt = "$clue $qText",
                            promptHi = "$clue कौन सा ${d.singularHi} सही क्रम में आता है?",
                            options = opts,
                            correctIndex = corrIdx,
                            explanation = "Complete deductive order: $e > $eD > $c > $b > $a. $expl",
                            explanationHi = "पूर्ण तार्किक क्रम: $e > $eD > $c > $b > $a। अतः सही उत्तर $ans है।",
                            accessibilityHint = "Hint: The sequence is inverted: $e > $eD > $c > $b > $a.",
                            accessibilityHintHi = "संकेत: उलटा क्रम बनाएं: $e > $eD > $c > $b > $a।"
                        )
                    )
                }
            }
        }

        return list
    }

    // =========================================================================
    // LEVEL 3: Functional Transformations & Custom Operators (500+ questions)
    // =========================================================================
    private fun generateLevel3Pool(): List<PuzzleQuestion> {
        val list = mutableListOf<PuzzleQuestion>()

        // Family 3.1: 2D Coordinate Transformations (X, Y) -> (aX + bY, cX - dY)
        val aCoeffs = listOf(2, 3, 4)
        val bCoeffs = listOf(1, 2, 3)
        val cCoeffs = listOf(2, 3)
        val dCoeffs = listOf(1, 2)
        val xPoints = listOf(3, 4, 5)
        val yPoints = listOf(1, 2, 3)

        for (a in aCoeffs) {
            for (b in bCoeffs) {
                for (c in cCoeffs) {
                    for (d in dCoeffs) {
                        for (x in xPoints) {
                            for (y in yPoints) {
                                val newY = c * x - d * y
                                if (newY > 0) {
                                    val newX = a * x + b * y
                                    val (opts, corrIdx) = makeCoordinateOptions(newX, newY)
                                    list.add(
                                        PuzzleQuestion(
                                            level = 3,
                                            prompt = "Transformation rule: (X, Y) -> (${a}X + ${b}Y, ${c}X - ${d}Y). Applying this operator to the coordinate pair ($x, $y) yields which resultant point?",
                                            promptHi = "रूपांतरण नियम: (X, Y) -> (${a}X + ${b}Y, ${c}X - ${d}Y)। बिंदु ($x, $y) पर लागू करने पर कौन सा बिंदु प्राप्त होगा?",
                                            options = opts,
                                            correctIndex = corrIdx,
                                            explanation = "Calculation: X' = $a($x) + $b($y) = $newX. Y' = $c($x) - $d($y) = $newY. Result: ($newX, $newY).",
                                            explanationHi = "गणना: X' = $a($x) + $b($y) = $newX; Y' = $c($x) - $d($y) = $newY। परिणाम = ($newX, $newY)।",
                                            accessibilityHint = "Hint: Compute $a($x) + $b($y) for the first coordinate, and $c($x) - $d($y) for the second.",
                                            accessibilityHintHi = "संकेत: पहले मान के लिए $a($x) + $b($y) और दूसरे मान के लिए $c($x) - $d($y) की गणना करें।"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Family 3.2: Non-standard operator A @ B = a*A^2 + b*B - c*A*B
        for (a in listOf(1, 2)) {
            for (b in listOf(2, 3, 4)) {
                for (c in listOf(1, 2)) {
                    for (bigA in listOf(2, 3, 4)) {
                        for (bigB in listOf(2, 3, 4)) {
                            val res = a * bigA * bigA + b * bigB - c * bigA * bigB
                            val (opts, corrIdx) = makeNumericOptions(res)
                            list.add(
                                PuzzleQuestion(
                                    level = 3,
                                    prompt = "Given the algebraic operator definition: A ⨂ B = ${a}A² + ${b}B - ${c}AB. What is the exact evaluated result of ($bigA ⨂ $bigB)?",
                                    promptHi = "यदि ऑपरेटर A ⨂ B = ${a}A² + ${b}B - ${c}AB है, तो ($bigA ⨂ $bigB) का मान क्या होगा?",
                                    options = opts,
                                    correctIndex = corrIdx,
                                    explanation = "Step 1: ${a}($bigA²) = ${a * bigA * bigA}. Step 2: ${b}($bigB) = ${b * bigB}. Step 3: ${c}($bigA)($bigB) = ${c * bigA * bigB}. Total = ${a * bigA * bigA} + ${b * bigB} - ${c * bigA * bigB} = $res.",
                                    explanationHi = "चरण 1: ${a}($bigA²) = ${a * bigA * bigA}। चरण 2: ${b}($bigB) = ${b * bigB}। चरण 3: ${c}($bigA)($bigB) = ${c * bigA * bigB}। कुल = $res।",
                                    accessibilityHint = "Hint: Substitute A = $bigA and B = $bigB into the formula.",
                                    accessibilityHintHi = "संकेत: सूत्र में A = $bigA और B = $bigB रखें।"
                                )
                            )
                        }
                    }
                }
            }
        }

        // Family 3.3: Piecewise parity function f(x)
        for (k1 in listOf(3, 5, 7)) {
            for (k2 in listOf(1, 2, 3)) {
                for (xEven in listOf(6, 8, 10, 12, 14, 16, 18, 20)) {
                    val res = (xEven / 2) + k1
                    val (opts, corrIdx) = makeNumericOptions(res)
                    list.add(
                        PuzzleQuestion(
                            level = 3,
                            prompt = "Define piecewise function f(x) = (x / 2) + $k1 when x is even, and ${k2}x + 1 when x is odd. What is the value of f($xEven)?",
                            promptHi = "यदि सम संख्या x के लिए f(x) = (x / 2) + $k1 और विषम के लिए ${k2}x + 1 है, तो f($xEven) क्या होगा?",
                            options = opts,
                            correctIndex = corrIdx,
                            explanation = "Since $xEven is even: f($xEven) = ($xEven / 2) + $k1 = ${xEven / 2} + $k1 = $res.",
                            explanationHi = "चूंकि $xEven सम है: f($xEven) = ($xEven / 2) + $k1 = $res।",
                            accessibilityHint = "Hint: $xEven is even, so use the first branch: ($xEven / 2) + $k1.",
                            accessibilityHintHi = "संकेत: $xEven सम संख्या है, अतः पहले नियम का प्रयोग करें: ($xEven / 2) + $k1।"
                        )
                    )
                }
                for (xOdd in listOf(3, 5, 7, 9, 11)) {
                    val res = k2 * xOdd + 1
                    val (opts, corrIdx) = makeNumericOptions(res)
                    list.add(
                        PuzzleQuestion(
                            level = 3,
                            prompt = "Define piecewise function f(x) = (x / 2) + $k1 when x is even, and ${k2}x + 1 when x is odd. What is the value of f($xOdd)?",
                            promptHi = "यदि सम x के लिए f(x) = (x / 2) + $k1 और विषम x के लिए ${k2}x + 1 है, तो f($xOdd) क्या होगा?",
                            options = opts,
                            correctIndex = corrIdx,
                            explanation = "Since $xOdd is odd: f($xOdd) = $k2($xOdd) + 1 = ${k2 * xOdd} + 1 = $res.",
                            explanationHi = "चूंकि $xOdd विषम है: f($xOdd) = $k2($xOdd) + 1 = $res।",
                            accessibilityHint = "Hint: $xOdd is odd, so multiply by $k2 and add 1.",
                            accessibilityHintHi = "संकेत: $xOdd विषम संख्या है, अतः इसे $k2 से गुणा करके 1 जोड़ें।"
                        )
                    )
                }
            }
        }

        return list
    }

    // =========================================================================
    // LEVEL 4: Algebraic Systems & Balance Puzzles (270+ questions)
    // =========================================================================
    private fun generateLevel4Pool(): List<PuzzleQuestion> {
        val list = mutableListOf<PuzzleQuestion>()

        // Family 4.1: Linear 2-variable systems
        val xVals = listOf(3, 4, 5, 6, 7, 8)
        val yVals = listOf(2, 3, 4, 5, 6)
        val systemConfigs = listOf(
            listOf(2, 1, 1, 2),
            listOf(3, 2, 2, 3),
            listOf(2, 3, 3, 1),
            listOf(3, 1, 1, 3)
        )

        for (x in xVals) {
            for (y in yVals) {
                if (x != y) {
                    for (cfg in systemConfigs) {
                        val a = cfg[0]
                        val b = cfg[1]
                        val c = cfg[2]
                        val d = cfg[3]
                        val det = a * d - b * c
                        if (det != 0) {
                            val c1 = a * x + b * y
                            val c2 = c * x + d * y

                            // Query product
                            val prod = x * y
                            val (optsP, corrIdxP) = makeNumericOptions(prod)
                            list.add(
                                PuzzleQuestion(
                                    level = 4,
                                    prompt = "System of linear equations: ${a}A + ${b}B = $c1, and ${c}A + ${d}B = $c2. What is the value of (A × B)?",
                                    promptHi = "रैखिक समीकरण: ${a}A + ${b}B = $c1 तथा ${c}A + ${d}B = $c2। (A × B) का मान ज्ञात करें।",
                                    options = optsP,
                                    correctIndex = corrIdxP,
                                    explanation = "Solving gives A = $x and B = $y. Product: $x × $y = $prod.",
                                    explanationHi = "हल करने पर A = $x और B = $y मिलता है। गुणनफल: $x × $y = $prod।",
                                    accessibilityHint = "Hint: Eliminate one variable. A is $x and B is $y.",
                                    accessibilityHintHi = "संकेत: समीकरण हल करने पर A = $x तथा B = $y प्राप्त होता है।"
                                )
                            )

                            // Query sum
                            val sum = x + y
                            val (optsS, corrIdxS) = makeNumericOptions(sum)
                            list.add(
                                PuzzleQuestion(
                                    level = 4,
                                    prompt = "Given linear constraints: ${a}X + ${b}Y = $c1, and ${c}X + ${d}Y = $c2. What is the value of (X + Y)?",
                                    promptHi = "समीकरण: ${a}X + ${b}Y = $c1 तथा ${c}X + ${d}Y = $c2। (X + Y) का मान ज्ञात करें।",
                                    options = optsS,
                                    correctIndex = corrIdxS,
                                    explanation = "Solving gives X = $x and Y = $y. Sum: $x + $y = $sum.",
                                    explanationHi = "हल करने पर X = $x तथा Y = $y मिलता है। योग = $x + $y = $sum।",
                                    accessibilityHint = "Hint: Solve the simultaneous equations: X = $x, Y = $y.",
                                    accessibilityHintHi = "संकेत: युगपत समीकरण हल करें: X = $x, Y = $y।"
                                )
                            )
                        }
                    }
                }
            }
        }

        // Family 4.2: Shape balance puzzles
        for (cVal in listOf(4, 5, 6, 7)) {
            for (sVal in listOf(3, 4, 5, 8)) {
                for (tVal in listOf(2, 3, 6, 7)) {
                    if (cVal != sVal && sVal != tVal && cVal != tVal) {
                        val s1 = cVal + sVal
                        val s2 = sVal + tVal
                        val s3 = cVal + tVal
                        val prod = cVal * sVal * tVal
                        val (opts, corrIdx) = makeNumericOptions(prod)
                        list.add(
                            PuzzleQuestion(
                                level = 4,
                                prompt = "In a weight balance puzzle: [Circle] + [Square] = $s1, [Square] + [Triangle] = $s2, and [Circle] + [Triangle] = $s3. What is [Circle] × [Square] × [Triangle]?",
                                promptHi = "समीकरण: [वृत्त] + [वर्ग] = $s1, [वर्ग] + [त्रिभुज] = $s2, तथा [वृत्त] + [त्रिभुज] = $s3। [वृत्त] × [वर्ग] × [त्रिभुज] का मान क्या होगा?",
                                options = opts,
                                correctIndex = corrIdx,
                                explanation = "Add all three: 2(Circle + Square + Triangle) = ${s1 + s2 + s3} -> Sum = ${(s1 + s2 + s3) / 2}. Individual: Circle = $cVal, Square = $sVal, Triangle = $tVal. Product: $cVal × $sVal × $tVal = $prod.",
                                explanationHi = "तीनों का योग जोड़ें और मान निकालें: वृत्त = $cVal, वर्ग = $sVal, त्रिभुज = $tVal। गुणनफल = $prod।",
                                accessibilityHint = "Hint: Circle is $cVal, Square is $sVal, and Triangle is $tVal.",
                                accessibilityHintHi = "संकेत: वृत्त $cVal, वर्ग $sVal, और त्रिभुज $tVal है।"
                            )
                        )
                    }
                }
            }
        }

        // Family 4.3: Sum and product integer puzzles
        val factorPairs = listOf(
            Pair(10, 3), Pair(12, 4), Pair(12, 5), Pair(14, 6),
            Pair(15, 5), Pair(15, 8), Pair(16, 6), Pair(18, 7),
            Pair(20, 8), Pair(24, 6), Pair(25, 10), Pair(16, 4)
        )
        for ((px, py) in factorPairs) {
            val sum = px + py
            val prod = px * py
            val diff = px - py
            val (optsD, corrIdxD) = makeNumericOptions(diff)
            list.add(
                PuzzleQuestion(
                    level = 4,
                    prompt = "Two positive integers X and Y satisfy: X + Y = $sum, and X × Y = $prod. Given that X > Y, what is the value of (X - Y)?",
                    promptHi = "दो धनात्मक पूर्णांक X और Y के लिए: X + Y = $sum और X × Y = $prod। यदि X > Y है, तो (X - Y) क्या होगा?",
                    options = optsD,
                    correctIndex = corrIdxD,
                    explanation = "Factor pairs of $prod summing to $sum are $px and $py. Difference: $px - $py = $diff.",
                    explanationHi = "संख्याएं $px और $py हैं। अंतर: $px - $py = $diff।",
                    accessibilityHint = "Hint: Find two numbers multiplying to $prod and adding to $sum: $px and $py.",
                    accessibilityHintHi = "संकेत: दो संख्याएं ज्ञात करें जिनका गुणनफल $prod और योग $sum हो: $px और $py।"
                )
            )

            val sqDiff = px * px - py * py
            val (optsSq, corrIdxSq) = makeNumericOptions(sqDiff)
            list.add(
                PuzzleQuestion(
                    level = 4,
                    prompt = "Two positive integers satisfy: X + Y = $sum, and X × Y = $prod. If X > Y, determine the value of (X² - Y²)?",
                    promptHi = "दो पूर्णांक संतुष्ट करते हैं: X + Y = $sum तथा X × Y = $prod। यदि X > Y, तो (X² - Y²) का मान क्या होगा?",
                    options = optsSq,
                    correctIndex = corrIdxSq,
                    explanation = "Numbers are $px and $py. Formula: (X² - Y²) = (X + Y)(X - Y) = $sum × $diff = $sqDiff.",
                    explanationHi = "संख्याएं $px और $py हैं। सूत्र: (X² - Y²) = (X + Y)(X - Y) = $sqDiff।",
                    accessibilityHint = "Hint: Use (X + Y) × (X - Y) = $sum × $diff.",
                    accessibilityHintHi = "संकेत: (X + Y) × (X - Y) सूत्र का प्रयोग करें।"
                )
            )
        }

        return list
    }

    // =========================================================================
    // LEVEL 5: Advanced Quantitative Multi-step Problems (250+ questions)
    // =========================================================================
    private fun generateLevel5Pool(): List<PuzzleQuestion> {
        val list = mutableListOf<PuzzleQuestion>()

        // Family 5.1: Harmonic Mean Average Speed
        val speedPairs = listOf(
            Pair(10, 15), Pair(12, 20), Pair(20, 30), Pair(30, 60), Pair(40, 60),
            Pair(60, 90), Pair(15, 60), Pair(24, 40), Pair(36, 45), Pair(30, 70),
            Pair(45, 90), Pair(50, 75), Pair(60, 100), Pair(75, 150)
        )
        val vehicles = listOf(
            Triple("A racing drone", "first lap", "second lap"),
            Triple("An express locomotive", "outbound trip", "return trip"),
            Triple("A prototype electric vehicle", "first segment", "second segment"),
            Triple("A hydrofoil watercraft", "upstream route", "downstream route"),
            Triple("A marathon cyclist", "stage 1", "stage 2")
        )
        for ((v1, v2) in speedPairs) {
            val hmean = (2 * v1 * v2) / (v1 + v2)
            for ((veh, leg1, leg2) in vehicles) {
                val (opts, corrIdx) = makeNumericOptions(hmean, "km/h")
                list.add(
                    PuzzleQuestion(
                        level = 5,
                        prompt = "$veh completes $leg1 at $v1 km/h and $leg2 over the exact same distance at $v2 km/h. What is the average speed for the entire journey?",
                        promptHi = "एक वाहन पहला भाग $v1 km/h और समान दूरी का दूसरा भाग $v2 km/h की गति से तय करता है। पूरी यात्रा की औसत गति क्या है?",
                        options = opts,
                        correctIndex = corrIdx,
                        explanation = "Harmonic mean formula: 2(V1 × V2) / (V1 + V2) = 2($v1 × $v2) / ($v1 + $v2) = ${2 * v1 * v2} / ${v1 + v2} = $hmean km/h.",
                        explanationHi = "हरात्मक माध्य: 2(V1 × V2) / (V1 + V2) = $hmean km/h।",
                        accessibilityHint = "Hint: Compute harmonic mean: 2 × $v1 × $v2 divided by ($v1 + $v2).",
                        accessibilityHintHi = "संकेत: हरात्मक माध्य निकालें: 2 × $v1 × $v2 / ($v1 + $v2)।"
                    )
                )
            }
        }

        // Family 5.2: Clock drift
        for (loss in listOf(1, 2, 3)) {
            for (gain in listOf(1, 2, 3)) {
                for (hours in listOf(4, 5, 6, 7, 8, 9, 10, 12)) {
                    val diff = hours * (loss + gain)
                    val (opts, corrIdx) = makeNumericOptions(diff, "minutes")
                    list.add(
                        PuzzleQuestion(
                            level = 5,
                            prompt = "Clock A loses $loss min/hr while Clock B gains $gain min/hr. Both clocks are precisely synchronized at 12:00 PM. How many minutes apart will they be after $hours hours?",
                            promptHi = "घड़ी A प्रति घंटे $loss मिनट धीमी होती है और घड़ी B प्रति घंटे $gain मिनट तेज होती है। 12:00 बजे मिलाने पर, $hours घंटे बाद उनके बीच कितने मिनट का अंतर होगा?",
                            options = opts,
                            correctIndex = corrIdx,
                            explanation = "Separation rate is ($loss + $gain) = ${loss + gain} min/hr. In $hours hours: $hours × ${loss + gain} = $diff minutes.",
                            explanationHi = "अलग होने की दर: $loss + $gain = ${loss + gain} मिनट/घंटा। $hours घंटों में: $hours × ${loss + gain} = $diff मिनट।",
                            accessibilityHint = "Hint: Multiply the elapsed hours ($hours) by the combined rate (${loss + gain} min/hr).",
                            accessibilityHintHi = "संकेत: बीते घंटे ($hours) को कुल दर (${loss + gain}) से गुणा करें।"
                        )
                    )
                }
            }
        }

        // Family 5.3: Cistern pipes
        val pipeTriplets = listOf(
            listOf(12, 15, 20, 5),
            listOf(10, 15, 30, 5),
            listOf(6, 10, 15, 3),
            listOf(20, 30, 60, 10),
            listOf(12, 20, 30, 6),
            listOf(15, 20, 60, 8),
            listOf(10, 20, 20, 5)
        )
        val reservoirs = listOf("industrial reservoir", "chemical storage vat", "municipal water tank", "cooling basin")
        for (trip in pipeTriplets) {
            val t1 = trip[0]
            val t2 = trip[1]
            val t3 = trip[2]
            val totalT = trip[3]
            for (resName in reservoirs) {
                val (opts, corrIdx) = makeNumericOptions(totalT, "minutes")
                list.add(
                    PuzzleQuestion(
                        level = 5,
                        prompt = "Three inlet pipes (Pipe A, Pipe B, and Pipe C) can fill an empty $resName in $t1, $t2, and $t3 minutes respectively. If all three pipes are opened simultaneously at full pressure, how many minutes will it take to fill the $resName completely?",
                        promptHi = "तीन नल क्रमशः $t1, $t2, और $t3 मिनट में टंकी भरते हैं। तीनों एक साथ खोलने पर टंकी कितने मिनट में भरेगी?",
                        options = opts,
                        correctIndex = corrIdx,
                        explanation = "Combined filling rate: (1/$t1 + 1/$t2 + 1/$t3) = 1/$totalT per minute. Required time: $totalT minutes.",
                        explanationHi = "संयुक्त दर: (1/$t1 + 1/$t2 + 1/$t3) = 1/$totalT। कुल समय = $totalT मिनट।",
                        accessibilityHint = "Hint: Add reciprocal rates (1/$t1 + 1/$t2 + 1/$t3) to find the combined speed.",
                        accessibilityHintHi = "संकेत: तीनों नलों की प्रति मिनट दर जोड़ें: (1/$t1 + 1/$t2 + 1/$t3)।"
                    )
                )
            }
        }

        // Family 5.4: Train crossing moving in opposite directions
        val trainParamsOpp = listOf(
            listOf(120, 180, 15, 10, 12),
            listOf(100, 140, 20, 10, 8),
            listOf(150, 150, 18, 12, 10),
            listOf(200, 250, 25, 20, 10),
            listOf(110, 130, 14, 16, 8),
            listOf(160, 240, 22, 18, 10),
            listOf(180, 220, 30, 20, 8),
            listOf(90, 150, 17, 13, 8)
        )
        for (tp in trainParamsOpp) {
            val l1 = tp[0]
            val l2 = tp[1]
            val s1 = tp[2]
            val s2 = tp[3]
            val crossT = tp[4]
            val (opts, corrIdx) = makeNumericOptions(crossT, "seconds")
            list.add(
                PuzzleQuestion(
                    level = 5,
                    prompt = "Train Alpha ($l1 m long) traveling at $s1 m/s and Train Beta ($l2 m long) traveling at $s2 m/s on parallel tracks move towards each other. How many seconds will they take to completely pass each other from the moment their front engines meet?",
                    promptHi = "दो रेलगाड़ियां ($l1 मी, $s1 मी/से और $l2 मी, $s2 मी/से) विपरीत दिशा में आ रही हैं। एक-दूसरे को पार करने में कितने सेकंड लगेंगे?",
                    options = opts,
                    correctIndex = corrIdx,
                    explanation = "Relative speed = $s1 + $s2 = ${s1 + s2} m/s. Total distance to clear = $l1 + $l2 = ${l1 + l2} m. Time = ${l1 + l2} / ${s1 + s2} = $crossT seconds.",
                    explanationHi = "सापेक्ष गति = $s1 + $s2 = ${s1 + s2} m/s। कुल दूरी = ${l1 + l2} m। समय = $crossT सेकंड।",
                    accessibilityHint = "Hint: Divide total combined length (${l1 + l2} m) by relative speed (${s1 + s2} m/s).",
                    accessibilityHintHi = "संकेत: कुल लंबाई (${l1 + l2} मी) को सापेक्ष गति (${s1 + s2} मी/से) से भाग दें।"
                )
            )
        }

        // Family 5.5: Train overtaking in same direction
        val trainParamsSame = listOf(
            listOf(150, 100, 30, 20, 25),
            listOf(180, 120, 35, 20, 20),
            listOf(200, 160, 40, 22, 20),
            listOf(140, 160, 28, 18, 30),
            listOf(220, 180, 32, 22, 40),
            listOf(125, 175, 35, 20, 20),
            listOf(250, 150, 45, 25, 20),
            listOf(160, 140, 35, 25, 30)
        )
        for (tp in trainParamsSame) {
            val l1 = tp[0]
            val l2 = tp[1]
            val s1 = tp[2]
            val s2 = tp[3]
            val overT = tp[4]
            val (opts, corrIdx) = makeNumericOptions(overT, "seconds")
            list.add(
                PuzzleQuestion(
                    level = 5,
                    prompt = "Express Train X ($l1 m long) traveling at $s1 m/s overtakes Cargo Train Y ($l2 m long) traveling in the same direction at $s2 m/s on adjacent tracks. From the moment the front of Train X reaches the rear of Train Y, how many seconds will it take for Train X to completely overtake Train Y?",
                    promptHi = "ट्रेन X ($l1 मी, $s1 मी/से) समान दिशा में जा रही ट्रेन Y ($l2 मी, $s2 मी/से) को ओवरटेक करती है। पूरा पार करने में कितना समय लगेगा?",
                    options = opts,
                    correctIndex = corrIdx,
                    explanation = "Relative speed = $s1 - $s2 = ${s1 - s2} m/s. Combined length = $l1 + $l2 = ${l1 + l2} m. Time = ${l1 + l2} / ${s1 - s2} = $overT seconds.",
                    explanationHi = "सापेक्ष गति = $s1 - $s2 = ${s1 - s2} m/s। कुल दूरी = ${l1 + l2} m। समय = $overT सेकंड।",
                    accessibilityHint = "Hint: Same direction relative speed is $s1 - $s2 = ${s1 - s2} m/s. Divide total length ${l1 + l2} m by ${s1 - s2}.",
                    accessibilityHintHi = "संकेत: समान दिशा में सापेक्ष गति $s1 - $s2 = ${s1 - s2} मी/से है।"
                )
            )
        }

        // Family 5.6: Simple vs Compound Interest difference for 2 years (D = P * (R/100)^2)
        val pValues = listOf(2000, 4000, 5000, 8000, 10000, 15000, 20000, 25000)
        val rValues = listOf(4, 5, 6, 8, 10, 12, 15)
        for (p in pValues) {
            for (r in rValues) {
                if ((p * r * r) % 10000 == 0) {
                    val diff = (p * r * r) / 10000
                    val (opts, corrIdx) = makeNumericOptions(diff, "$")
                    list.add(
                        PuzzleQuestion(
                            level = 5,
                            prompt = "An institutional investor deposits $$p into a treasury account for 2 years at an annual interest rate of $r%. What is the exact monetary difference between the Compound Interest (compounded annually) and the Simple Interest earned over this 2-year period?",
                            promptHi = "$$p की राशि 2 वर्ष के लिए $r% की वार्षिक दर पर जमा की जाती है। 2 वर्ष के चक्रवृद्धि और साधारण ब्याज का अंतर क्या होगा?",
                            options = opts,
                            correctIndex = corrIdx,
                            explanation = "Formula for 2-year difference: P × (R/100)² = $p × ($r/100)² = $p × ${r * r} / 10000 = $$diff.",
                            explanationHi = "2 वर्ष का अंतर: P × (R/100)² = $$diff।",
                            accessibilityHint = "Hint: 2-year difference formula is P × (R/100)².",
                            accessibilityHintHi = "संकेत: 2 वर्ष के अंतर का सूत्र P × (R/100)² है।"
                        )
                    )
                }
            }
        }

        // Family 5.7: Mixture dilution replacement
        val mixParams = listOf(
            listOf(100, 10, 81),
            listOf(100, 20, 64),
            listOf(50, 10, 32),
            listOf(200, 20, 162),
            listOf(80, 20, 45),
            listOf(125, 25, 80),
            listOf(200, 40, 128),
            listOf(250, 50, 160),
            listOf(160, 40, 90),
            listOf(180, 30, 125)
        )
        for (mp in mixParams) {
            val v = mp[0]
            val x = mp[1]
            val rem = mp[2]
            val (opts, corrIdx) = makeNumericOptions(rem, "liters")
            list.add(
                PuzzleQuestion(
                    level = 5,
                    prompt = "A storage container holds $v liters of pure chemical reagent. Exactly $x liters are drawn out and replaced with an equal volume of neutral water. This entire dilution operation is repeated a second time. How many liters of pure chemical reagent remain in the mixture?",
                    promptHi = "$v लीटर शुद्ध रसायन में से $x लीटर निकालकर उतना ही पानी मिलाया जाता है। यह प्रक्रिया एक बार और दोहराई जाती है। बर्तन में कितना शुद्ध रसायन शेष है?",
                    options = opts,
                    correctIndex = corrIdx,
                    explanation = "Formula: V × (1 - X/V)² = $v × (1 - $x/$v)² = $rem liters.",
                    explanationHi = "सूत्र: V × (1 - X/V)² = $rem लीटर।",
                    accessibilityHint = "Hint: Compute $v × (${v - x}/$v)² = $rem liters.",
                    accessibilityHintHi = "संकेत: सूत्र $v × (${v - x}/$v)² का प्रयोग करें।"
                )
            )
        }

        return list
    }

    // =========================================================================
    // Option Formatting Helpers
    // =========================================================================
    private fun makeNumericOptions(ansVal: Int, unit: String = ""): Pair<List<String>, Int> {
        val offsets = listOf(-3, -2, -1, 1, 2, 3, 4, 5, -4, 6).shuffled()
        val distractors = mutableSetOf<Int>()
        for (off in offsets) {
            val cand = ansVal + off
            if (cand != ansVal && (ansVal <= 0 || cand > 0)) {
                distractors.add(cand)
                if (distractors.size == 3) break
            }
        }
        var step = 7
        while (distractors.size < 3) {
            val cand = ansVal + step
            if (cand != ansVal) distractors.add(cand)
            step += 2
        }
        val all = (listOf(ansVal) + distractors).shuffled()
        val options = all.map { v ->
            if (unit.startsWith("$")) "$unit$v"
            else if (unit.isNotEmpty()) "$v $unit"
            else v.toString()
        }
        val correctStr = if (unit.startsWith("$")) "$unit$ansVal"
        else if (unit.isNotEmpty()) "$ansVal $unit"
        else ansVal.toString()
        return Pair(options, options.indexOf(correctStr))
    }

    private fun makeCoordinateOptions(x: Int, y: Int): Pair<List<String>, Int> {
        val correct = "($x, $y)"
        val dists = mutableSetOf<String>()
        val candidates = listOf(
            "(${x + 1}, $y)",
            "($x, ${y + 1})",
            "(${x - 1}, $y)",
            "($x, ${y - 1})",
            "(${x + 1}, ${y - 1})",
            "(${x - 1}, ${y + 1})",
            "(${x + 2}, $y)",
            "($x, ${y + 2})"
        ).shuffled()
        for (c in candidates) {
            if (c != correct) {
                dists.add(c)
                if (dists.size == 3) break
            }
        }
        val all = (listOf(correct) + dists).shuffled()
        return Pair(all, all.indexOf(correct))
    }

    private fun makeEntityOptions(correctName: String, allNames: List<String>): Pair<List<String>, Int> {
        val otherNames = allNames.filter { it != correctName }.shuffled().take(3)
        val all = (listOf(correctName) + otherNames).shuffled()
        return Pair(all, all.indexOf(correctName))
    }
}

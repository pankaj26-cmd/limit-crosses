package com.limitcrosses.app.data.repository

import com.limitcrosses.app.core.model.CosmosCategory
import com.limitcrosses.app.core.model.CosmosTopic
import com.limitcrosses.app.domain.repository.CosmosRepository

class CosmosRepositoryImpl : CosmosRepository {

    private val curatedTopics = listOf(
        CosmosTopic(
            id = "cosmos-1",
            title = "The Observable Universe & Cosmic Web",
            titleHi = "अवलोकनीय ब्रह्मांड और कॉस्मिक जाल",
            category = CosmosCategory.UNIVERSE,
            summary = "The observable universe spans roughly 93 billion light-years across, structured into vast cosmic filaments and immense voids.",
            summaryHi = "अवलोकनीय ब्रह्मांड लगभग 93 अरब प्रकाश-वर्ष में फैला है, जो विशाल कॉस्मिक फिलामेंट्स और रिक्त स्थानों में संरचित है।",
            fullDescription = "According to cosmological measurements from NASA's Wilkinson Microwave Anisotropy Probe (WMAP) and ESA's Planck observatory, the universe is approximately 13.8 billion years old. Matter forms an intricate cosmic web, where galaxies reside along gravitational filaments of dark matter surrounding immense cosmic voids.",
            fullDescriptionHi = "नासा के WMAP और ईएसए के प्लैंक वेधशाला के मापों के अनुसार, ब्रह्मांड लगभग 13.8 अरब वर्ष पुराना है। पदार्थ एक जटिल कॉस्मिक जाल बनाता है, जहां आकाशगंगाएं विशाल शून्यों को घेरने वाले डार्क मैटर के फिलामेंट्स के साथ स्थित हैं।",
            sourceName = "NASA / ESA Planck Mission",
            sourceUrl = "https://www.esa.int/Science_Exploration/Space_Science/Planck",
            lastVerifiedDate = "2026-03-15"
        ),
        CosmosTopic(
            id = "cosmos-2",
            title = "The Andromeda & Milky Way Collision",
            titleHi = "एंड्रोमेडा और आकाशगंगा का महामिलन",
            category = CosmosCategory.GALAXIES,
            summary = "In approximately 4.5 billion years, our home galaxy and the Andromeda spiral will merge into a giant elliptical galaxy.",
            summaryHi = "लगभग 4.5 अरब वर्षों में, हमारी आकाशगंगा और एंड्रोमेडा सर्पिल मिलकर एक विशाल अण्डाकार आकाशगंगा बन जाएंगे।",
            fullDescription = "Hubble Space Telescope measurements demonstrate that Andromeda (M31) is approaching the Milky Way at roughly 110 kilometers per second. Because stellar distances are so vast relative to stellar radii, individual stars will virtually never collide during the multi-billion-year galactic coalescence.",
            fullDescriptionHi = "हबल टेलीस्कोप के माप दर्शाते हैं कि एंड्रोमेडा लगभग 110 किमी प्रति सेकंड की गति से हमारी ओर आ रही है। तारों के बीच अत्यधिक दूरी के कारण तारों का आपसी टकराव नगण्य होगा।",
            sourceName = "NASA Goddard Space Flight Center",
            sourceUrl = "https://science.nasa.gov/mission/hubble/",
            lastVerifiedDate = "2026-05-10"
        ),
        CosmosTopic(
            id = "cosmos-3",
            title = "Stellar Nucleosynthesis & Supernovae",
            titleHi = "तारकीय परमाणु-संश्लेषण और सुपरनोवा",
            category = CosmosCategory.STARS,
            summary = "Every element heavier than hydrogen and helium in our bodies was forged inside ancient stars and scattered across space in stellar explosions.",
            summaryHi = "हमारे शरीर में हाइड्रोजन और हीलियम से भारी प्रत्येक तत्व प्राचीन तारों के भीतर बना और सुपरनोवा विस्फोटों द्वारा अंतरिक्ष में फैला।",
            fullDescription = "Stars fuse light nuclei under immense core gravitational pressures. Massive stars forge elements up to iron in concentric shells before undergoing core collapse, triggering Type II supernovae that enrich the interstellar medium with carbon, oxygen, calcium, and gold.",
            fullDescriptionHi = "तारे गुरुत्वाकर्षण दबाव में हल्के नाभिकों को जोड़ते हैं। विशाल तारे लोहे तक के तत्व बनाते हैं और फिर कोर ढहने से सुपरनोवा विस्फोट होता है, जिससे कार्बन, ऑक्सीजन और सोना अंतरिक्ष में फैलता है।",
            sourceName = "NASA Astrophysics Division",
            sourceUrl = "https://science.nasa.gov/astrophysics/focus-areas/how-do-stars-form-and-evolve/",
            lastVerifiedDate = "2026-06-01"
        ),
        CosmosTopic(
            id = "cosmos-4",
            title = "Supermassive Black Holes & Event Horizons",
            titleHi = "सुपरमैसिव ब्लैक होल और घटना क्षितिज",
            category = CosmosCategory.BLACK_HOLES,
            summary = "Sagittarius A* at our galactic center holds 4.1 million solar masses within a radius smaller than Mercury's orbit.",
            summaryHi = "हमारी आकाशगंगा के केंद्र में सैजिटेरियस A* बुध की कक्षा से छोटे दायरे में 41 लाख सूर्यों का द्रव्यमान समेटे हुए है।",
            fullDescription = "The Event Horizon Telescope (EHT) collaboration, supported by NASA and European observatories, captured the direct radio silhouette of Sagittarius A*. Light passing within the photon sphere is trapped forever by extreme gravitational curvature predicted by Einstein's General Relativity.",
            fullDescriptionHi = "इवेंट होराइजन टेलीस्कोप ने सैजिटेरियस A* की रेडियो छवि कैप्चर की। फोटॉन क्षेत्र के भीतर जाने वाला प्रकाश आइंस्टीन के सामान्य सापेक्षता सिद्धांत के अनुसार हमेशा के लिए फंस जाता है।",
            sourceName = "Event Horizon Telescope / NASA",
            sourceUrl = "https://science.nasa.gov/astrophysics/focus-areas/black-holes/",
            lastVerifiedDate = "2026-07-22"
        ),
        CosmosTopic(
            id = "cosmos-5",
            title = "Europa & Enceladus Subsurface Oceans",
            titleHi = "यूरोपा और एन्सेलेडस के उप-सतही महासागर",
            category = CosmosCategory.PLANETS,
            summary = "Icy moons Europa (Jupiter) and Enceladus (Saturn) harbor liquid saltwater oceans warmed by tidal geothermal forces beneath ice shells.",
            summaryHi = "बर्फ से ढके यूरोपा और एन्सेलेडस चंद्रमाओं के नीचे ज्वारीय ताप से गर्म नमकीन पानी के गहरे महासागर छिपे हैं।",
            fullDescription = "NASA's Europa Clipper and ESA's JUICE (JUpiter ICy moons Explorer) missions investigate the habitability of these oceanic environments. Hydrothermal plumes detected by Cassini on Enceladus contain molecular hydrogen, organic carbon compounds, and silicates.",
            fullDescriptionHi = "नासा का यूरोपा क्लिपर और ईएसए का JUICE मिशन इन महासागरों में जीवन की संभावनाओं की जांच कर रहे हैं। कैसिनी ने एन्सेलेडस के फुहारों में कार्बनिक यौगिक और हाइड्रोजन पाया है।",
            sourceName = "ESA JUICE Mission / NASA Europa Clipper",
            sourceUrl = "https://www.esa.int/Science_Exploration/Space_Science/Juice",
            lastVerifiedDate = "2026-08-14"
        ),
        CosmosTopic(
            id = "cosmos-6",
            title = "James Webb Space Telescope (JWST)",
            titleHi = "जेम्स वेब स्पेस टेलीस्कोप (JWST)",
            category = CosmosCategory.MISSIONS,
            summary = "Operating 1.5 million kilometers from Earth at Lagrange Point 2, Webb observes the earliest luminous stars and galaxies formed after the Big Bang.",
            summaryHi = "पृथ्वी से 15 लाख किलोमीटर दूर L2 बिंदु पर तैनात वेब टेलीस्कोप बिग बैंग के बाद बनी शुरुआती आकाशगंगाओं का अध्ययन करता है।",
            fullDescription = "An international partnership between NASA, ESA, and CSA, Webb features a 6.5-meter gold-plated beryllium primary mirror and infrared cryogenic instruments (MIRI, NIRSpec, NIRCam) capable of peering through cosmic dust to analyze exoplanetary atmospheres.",
            fullDescriptionHi = "नासा, ईएसए और सीएसए की यह संयुक्त वेधशाला 6.5 मीटर के सोने की परत वाले दर्पण और अवरक्त उपकरणों से सुसज्जित है जो कॉस्मिक धूल के पार देखने में सक्षम हैं।",
            sourceName = "NASA / ESA / CSA JWST Mission",
            sourceUrl = "https://webb.nasa.gov/",
            lastVerifiedDate = "2026-09-30"
        )
    )

    override fun getTopics(category: CosmosCategory): List<CosmosTopic> {
        return if (category == CosmosCategory.ALL) {
            curatedTopics
        } else {
            curatedTopics.filter { it.category == category }
        }
    }

    override fun getTopicById(id: String): CosmosTopic? = curatedTopics.find { it.id == id }

    override fun getAllCategories(): List<CosmosCategory> = CosmosCategory.values().toList()
}


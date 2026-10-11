package com.example.polyglottyper.data.words

import com.example.polyglottyper.domain.model.Language

/**
 * Practice word pools. To add a language: add one [Language] enum entry
 * and one list here — every feature (generation, daily seed, rain, tests)
 * picks it up automatically.
 */
object WordLists {

    val wordsByLanguage: Map<Language, List<String>> = mapOf(
        Language.ENGLISH to ENGLISH,
        Language.SPANISH to SPANISH,
        Language.FRENCH to FRENCH,
        Language.GERMAN to GERMAN,
        Language.ITALIAN to ITALIAN,
        Language.PORTUGUESE to PORTUGUESE,
        Language.TURKISH to TURKISH,
    )

    fun forLanguage(language: Language): List<String> =
        wordsByLanguage.getValue(language)

    private fun String.toWords(): List<String> = trim().split(Regex("\\s+"))

    private val ENGLISH = """
        the be to of and a in that have it for not on with he as you do at this but his by from
        they we say her she or an will my one all would there their what so up out if about who
        get which go me when make can like time no just him know take people into year your good
        some could them see other than then now look only come its over think also back after
        use two how our work first well way even new want because any these give day most us
        house water world life child hand place week school story month right book word business
    """.trimIndent().toWords()

    private val SPANISH = """
        de la que el en y a los se del las un por con no una su para es al lo como más pero sus
        le ya o este sí porque esta entre cuando muy sin sobre también me hasta hay donde quien
        desde todo nos durante todos uno les ni contra otros ese eso ante ellos esto mí antes
        algunos qué unos yo otro él tanto esa estos mucho nada muchos cual poco ella estar estas
        algo nosotros tiempo día vida hombre mundo parte casa mujer niño ojos gente trabajo
        escuela música comida feliz grande pequeño hermoso mañana noche siempre nunca amigo
        ventana libro agua ciudad familia llegar preguntar escuchar contestar estudiar bailar
    """.trimIndent().toWords()

    private val FRENCH = """
        le la les un une des et ou mais donc pour dans sur avec sans chez avant après pendant
        toujours jamais souvent aujourd'hui demain hier matin soir nuit jour temps heure maison
        école travail ville monde vie ami famille enfant femme homme eau livre musique manger
        boire parler écrire lire grand petit beau bon mauvais nouveau vieux heureux triste
        facile difficile penser savoir voir aller venir faire dire prendre donner vouloir
        pouvoir devoir être avoir nous vous ils elles mon ton son notre votre leur ce cette
        ces qui que dont où très bien comme aussi alors chose seule chaque encore déjà
    """.trimIndent().toWords()

    private val GERMAN = """
        der die das den dem des ein eine einen einem und oder aber denn sondern weil dass wenn
        als wie in an auf mit von zu bei nach vor über unter zwischen für ohne um durch gegen
        seit ich du er sie es wir ihr man nicht kein sehr auch noch schon immer nie oft heute
        morgen gestern jetzt dann hier dort zeit tag nacht haus schule arbeit stadt welt leben
        freund familie kind frau mann wasser buch musik essen trinken sprechen schreiben lesen
        groß klein schön gut schlecht neu alt glücklich traurig leicht schwer denken wissen
        sehen gehen kommen machen sagen nehmen geben wollen können müssen
    """.trimIndent().toWords()

    private val ITALIAN = """
        il lo la i gli le un uno una di a da in con su per tra fra e o ma però perché se quando
        come che chi cui non più molto poco anche ancora sempre mai spesso oggi domani ieri
        adesso poi qui lì tempo giorno notte mattina casa scuola lavoro città mondo vita amico
        famiglia bambino donna uomo acqua libro musica mangiare bere parlare scrivere leggere
        grande piccolo bello buono cattivo nuovo vecchio felice triste facile difficile pensare
        sapere vedere andare venire fare dire prendere dare volere potere dovere
    """.trimIndent().toWords()

    private val PORTUGUESE = """
        o a os as um uma uns umas de do da dos das em no na nos nas por pelo pela com sem sob
        sobre entre para até desde e ou mas porém porque se quando como que quem cujo não mais
        muito pouco também ainda sempre nunca hoje amanhã ontem agora depois aqui ali tempo dia
        noite manhã casa escola trabalho cidade mundo vida amigo família criança mulher homem
        água livro música comer beber falar escrever ler grande pequeno bonito bom mau novo
        velho feliz triste fácil difícil pensar saber ver ir vir fazer dizer tomar dar querer
        poder dever
    """.trimIndent().toWords()

    // Every word uses only characters available on the Turkish F layout
    // (verified by KeyboardLayoutTest).
    private val TURKISH = """
        bir ve bu için çok ama daha gibi ile olarak sonra önce zaman yıl gün gece sabah akşam
        ev su hayat dünya insan çocuk kadın adam arkadaş aile anne baba kardeş şehir okul iş
        para yol sokak araba kitap müzik yemek ekmek çay kahve meyve hayvan kedi köpek kuş
        ağaç çiçek deniz dağ hava sıcak soğuk iyi kötü büyük küçük yeni eski güzel mutlu
        hızlı kolay zor
    """.trimIndent().toWords()
}

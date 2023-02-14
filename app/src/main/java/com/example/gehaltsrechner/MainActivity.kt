package com.example.gehaltsrechner

//import android.util.Log
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private lateinit var lohnEdit:EditText
    private lateinit var stundenEdit:EditText
    private lateinit var radioButtonJahr:RadioButton

    private lateinit var tvBruttoMonat:TextView
    private lateinit var tvBrutto13:TextView
    private lateinit var tvBrutto14:TextView
    private lateinit var tvBruttoJahr:TextView

    private lateinit var tvSVMonat:TextView
    private lateinit var tvSV13:TextView
    private lateinit var tvSV14:TextView
    private lateinit var tvSVJahr:TextView

    private lateinit var tvSteuerMonat:TextView
    private lateinit var tvSteuer13:TextView
    private lateinit var tvSteuer14:TextView
    private lateinit var tvSteuerJahr:TextView

    private lateinit var tvNettoMonat:TextView
    private lateinit var tvNetto13:TextView
    private lateinit var tvNetto14:TextView
    private lateinit var tvNettoJahr:TextView

    private lateinit var tableLayoutStundenlohn:TableLayout
    private lateinit var tableRowMarginal:TableRow
    private lateinit var tvStundeBrutto:TextView
    private lateinit var tvStundeNetto:TextView
    private lateinit var tvStundeMarginal:TextView

    private lateinit var tvAnmerkungen:TextView


    private var bruttoMonat = 0.0
    private var bruttoJahr = 0.0

    private var svMonat = 0.0
    private var sv13 = 0.0
    private var sv14 = 0.0
    private var svJahr = 0.0

    private var steuerLaufendeBezuege = 0.0
    private var steuerLaufendeBezuegeVielleichtNegativ = 0.0
    private var steuerMonat = 0.0
    private var steuer13 = 0.0
    private var steuer14 = 0.0

    private var steuerMonatGerundet = 0.0
    private var steuer13Gerundet = 0.0
    private var steuer14Gerundet = 0.0
    private var steuerJahr = 0.0

    private var rueckzahlungen = 0.0

    private var nettoMonat = 0.0
    private var netto13 = 0.0
    private var netto14 = 0.0
    private var nettoJahr = 0.0


    private var stundeNettoGerundet = 0.0
    private var stundeBruttoGerundet = 0.0
    private var stundeMarginalGerundet = 0.0

    private var svMonatUngerundet = 0.0 //die Ungerundets sind für den Marginallohn, damit dieser keine Rundungseffekte hat
    private var sv13Ungerundet = 0.0
    private var sv14Ungerundet = 0.0
    private var svJahrUngerundet = 0.0

    private var steuerLaufendeBezuegeUngerundet = 0.0
    private var steuerLaufendeBezuegeVielleichtNegativUngerundet = 0.0
    private var steuerMonatUngerundet = 0.0
    private var steuer13Ungerundet = 0.0
    private var steuer14Ungerundet = 0.0
    private var steuerJahrUngerundet = 0.0


    //private var rueckzahlungenUngerundet = 0.0

    private var nettoMonatUngerundet = 0.0
    private var netto13Ungerundet = 0.0
    private var netto14Ungerundet = 0.0
    private var nettoJahrUngerundet = 0.0

    private val ERR = 1.0.pow(-6)

    //private val TAG = "_________________________________________________________________________CUSTOM_LOG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (mode) {
            Configuration.UI_MODE_NIGHT_YES -> supportActionBar?.setLogo(R.mipmap.darkgrey_white_cash)
            else -> supportActionBar?.setLogo(R.mipmap.red_white_cash)
        }
        supportActionBar?.setDisplayUseLogoEnabled(true)

        lohnEdit = findViewById(R.id.lohnEdit)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE) //needed s.t. keyboard opens with the next command
        lohnEdit.requestFocus()

        findViews()
        tableLayoutStundenlohn.visibility = View.GONE

        waitForInput() //and fillTable()
    }

    private fun findViews(){
        stundenEdit = findViewById(R.id.stundenEdit)
        radioButtonJahr = findViewById(R.id.radioButtonJahr)

        tvBruttoMonat = findViewById(R.id.tvBruttoMonat)
        tvBrutto13 = findViewById(R.id.tvBrutto13)
        tvBrutto14 = findViewById(R.id.tvBrutto14)
        tvBruttoJahr = findViewById(R.id.tvBruttoJahr)

        tvSVMonat = findViewById(R.id.tvSVMonat)
        tvSV13 = findViewById(R.id.tvSV13)
        tvSV14 = findViewById(R.id.tvSV14)
        tvSVJahr = findViewById(R.id.tvSVJahr)

        tvSteuerMonat = findViewById(R.id.tvSteuerMonat)
        tvSteuer13 = findViewById(R.id.tvSteuer13)
        tvSteuer14 = findViewById(R.id.tvSteuer14)
        tvSteuerJahr = findViewById(R.id.tvSteuerJahr)

        tvNettoMonat = findViewById(R.id.tvNettoMonat)
        tvNetto13 = findViewById(R.id.tvNetto13)
        tvNetto14 = findViewById(R.id.tvNetto14)
        tvNettoJahr = findViewById(R.id.tvNettoJahr)


        tableLayoutStundenlohn = findViewById(R.id.tableLayoutStundenlohn)
        tableRowMarginal = findViewById(R.id.tableRowMarginal)
        tvStundeBrutto = findViewById(R.id.tvStundeBrutto)
        tvStundeNetto = findViewById(R.id.tvStundeNetto)
        tvStundeMarginal = findViewById(R.id.tvStundeMarginal)

        tvAnmerkungen = findViewById(R.id.tvAnmerkungen)
    }

    private fun waitForInput() {
        stundenEdit.setOnEditorActionListener { textView , actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                //Log.d(TAG, "Input done.")
                rechnen()
                true
            } else {
                //Log.d(TAG, "ERROR in isInputDone(); FALSE, actionId is $actionId")
                false
            }
        }
    }

    private fun rechnen(){
        /*
        Gehaltsrechner des Finanzministeriums:
        https://bruttonetto.azurewebsites.net/
         */

        //Log.d(TAG, "fillTable() invoked.")
        tvAnmerkungen.text = ""

        if (lohnEdit.text.toString().isEmpty() || lohnEdit.text.toString() == "."){
            //Log.d(TAG, "No number in first box.")
            return
        }


        brutto()
        sozialversicherungsabgaben()
        lohnsteuerLaufendeBezuege()
        lohnsteuerAnmerkungenRueckerstattung()
        lohnsteuerSonstigeBezuege()
        netto()
        tabelleFuellen()

        stundenlohn()



    }

    private fun brutto(){
        val brutto = round(100*lohnEdit.text.toString().toDouble())/100
        if (radioButtonJahr.isChecked) {
            bruttoMonat = round(100* brutto/14 )/100
            bruttoJahr = bruttoMonat*14
        } else {
            bruttoMonat = brutto
            bruttoJahr = bruttoMonat*14
        }
        bruttoJahr = round(100*bruttoJahr)/100
    }

    private fun sozialversicherungsabgaben(){
        /*
        Sozialversicherungsabgaben
        ----------------------------------------------
        https://www.wko.at/service/steuern/lohnverrechnung-abrechnung-dienstnehmer.html#heading_zu_1__bei_laufenden_Bezuegen
        Geringfügigkeitsgrenze: monatlich EUR 485,85 (erst wer mehr verdient, muss den vollen Anteil zahlen, bis zu dem Betrag: 0€
        Höchstbeitragsgrundlage: monatlich EUR 5.670,00
        Sonderzahlungen (Urlaubsgeld, Weihnachtsgeld) sind im Kalenderjahr bis zu einem Höchstbetrag von EUR 11.340,00 beitragspflichtig
        Beitragssatz Dienstnehmer: 18,12%
        bei Geringverdienern ist es geringer:
        Bis 1.790  	Reduktion um 3 %-punkte (immer sprunghaft, deshalb aufpassen bei der Gehaltsverhandlung)
        Über 1.790 bis 1.953 	Reduktion um 2 %
        Über 1.953 bis 2.117 	Reduktion um 1 %

        Liegt eine Sonderzahlung vor, wie z.B. Urlaubsgeld, Weihnachtsgeld, jährliche Leistungsprämie etc., unterliegt dieser Bezug nicht der Arbeiterkammerumlage und nicht dem Wohnbauförderungsbeitrag.
         Der Dienstnehmeranteil zur Sozialversicherung beträgt deshalb bei Arbeitern und Angestellten nur mehr 17,12 % (bzw. 16,12 %, 15,12 %, 14,12 %) Höchstbeitragsgrundlage für SZ = 11.100 EUR jährlich.

         Die SV-Beiträge errechnen sich monatlich, die Lohnsteuer aber jährlich (wichtig fürs Runden -> bei LSt den Jahresbetrag runden).

        ÖGK "Veränderliche Werte 2022":
        https://www.gesundheitskasse.at/cdscontent/?contentid=10007.868470&portal=oegkdgportal
        */


        when {
            bruttoMonat <= 485.85 -> {svMonat = 0.0; sv13 = 0.0}
            bruttoMonat <=1828 -> {svMonat = bruttoMonat*0.1512; sv13 = bruttoMonat*0.1412}
            bruttoMonat <=1994 -> {svMonat = bruttoMonat*0.1612; sv13 = bruttoMonat*0.1512}
            bruttoMonat <=2161 -> {svMonat = bruttoMonat*0.1712; sv13 = bruttoMonat*0.1612}
            bruttoMonat < 5670 -> {svMonat = bruttoMonat*0.1812; sv13 = bruttoMonat*0.1712} //"normal"
            bruttoMonat < 11340 -> {svMonat = 5670*0.1812; sv13 = bruttoMonat*0.1712}
            else -> {svMonat = 5670*0.1812; sv13 = 11340*0.1712}
        }
        svMonatUngerundet = svMonat
        svMonat = round(100*svMonat)/100
        sv13Ungerundet = sv13
        sv13 = round(100*sv13)/100


        if (2*sv13 >= 1941.41) {
            sv14 = 1941.41 - sv13 //reverse engineered von der offiziellen Rechnerwebsite; Quellen gibt es keine; 1941.41=11340*0.
            sv14Ungerundet = sv14
            sv14 = round(100*sv14)/100
        } else {
            sv14 = sv13
            sv14Ungerundet = sv14
        }

        svJahr = 12*svMonat + sv13 + sv14
        svJahr = round(100*(svJahr))/100

        svJahrUngerundet = 12*svMonatUngerundet + sv13Ungerundet + sv14Ungerundet
    }

    private fun lohnsteuerLaufendeBezuege(){
        /*
        Grenzsteuersätze:
        https://www.usp.gv.at/steuern-finanzen/einkommensteuer/tarifstufen-berechnungsformeln.html

        Einkommenssteuergesetz
        https://www.ris.bka.gv.at/NormDokument.wxe?Abfrage=Bundesnormen&Gesetzesnummer=10004570&Paragraf=33

        §33 (2) Von dem sich nach Abs. 1 ergebenden Betrag sind Absetzbeträge in folgender Reihenfolge abzuziehen:
         ...
                 2. Die Absetzbeträge nach Abs. 4 bis 6.
         ...
         (5) Bei Einkünften aus einem bestehenden Dienstverhältnis stehen folgende Absetzbeträge zu:
             1. Ein Verkehrsabsetzbetrag von 400 Euro jährlich.
         ...
             3. Der Verkehrsabsetzbetrag gemäß Z 1 oder 2 erhöht sich um 650 Euro (Zuschlag), wenn das Einkommen des Steuerpflichtigen 16 000 Euro im Kalenderjahr nicht übersteigt.
                Der Zuschlag vermindert sich zwischen Einkommen von 16 000 Euro und 24 500 Euro gleichmäßig einschleifend auf null.
         ...

        [§ 62.Beim Steuerabzug vom Arbeitslohn sind vor Anwendung des Lohnsteuertarifes (§ 66) vom Arbeitslohn abzuziehen:
             1. Der Pauschbetrag für Werbungskosten (§ 16 Abs. 3),
             2. der Pauschbetrag für Sonderausgaben (§ 18 Abs. 2),
         ...]

         Die Absetzbeträge sind Beträge, die in voller Höhe direkt von der Einkommensteuer abgezogen werden.
         Sie sind von Freibeträgen zu unterscheiden. Absetzbeträge werden von der Arbeitgeberin/vom Arbeitgeber bzw. von der pensionsauszahlenden Stelle abgezogen
         oder können selbst geltend gemacht werden.
         Absetzbeträge, die automatisch berücksichtigt werden, sind:
              Verkehrsabsetzbetrag
              Pensionistenabsetzbetrag
         Absetzbeträge, die beantragt werden müssen, sind: ...

         Freibeträge (z.B. Werbungskosten, Sonderausgaben) werden im Rahmen der Arbeitnehmerveranlagung
         von dem zu versteuerndem Einkommen abgezogen und vermindern somit die Bemessungsgrundlage.

         var absetzbetrag:Double=0.0
         val verkehrsAbsetzbetrag = 400.0
         absetzbetrag += verkehrsAbsetzbetrag

         var freibetraege:Double = 0.0
         val werbungskostenpauschale = 132.0 //https://www.arbeiterkammer.at/beratung/steuerundeinkommen/steuertipps/Werbungskosten.html
         val sonderausgabenPauschbetrag = 60.0  //§18 (2)
         val gebuehrECardNovember = 12.95 // Die E-Card wird immer im November abgebucht und wird steuerlich nicht auf alle Monate umgelegt.
         freibetraege += werbungskostenpauschale + sonderausgabenPauschbetrag + gebuehrECardNovember
        */

        val schwellwerte = listOf(11000, 18000, 31000, 60000, 90000, 1000000)
        val grenzsteuersaetze = listOf(0.0, .2, .325, .42, .48, .5, .55)

        var schwellwert:Double
        var grenzsteuersatz:Double

        val werbungskostenpauschale = 132.0 //https://www.arbeiterkammer.at/beratung/steuerundeinkommen/steuertipps/Werbungskosten.html
        val verkehrsAbsetzbetrag = 400.0

        fun berechneLohnsteuerLaufendeBezuege(zuVersteuerndesEinkommen:Double):Double {
            var steuer = 0.0
            var nochZuVersteuerndesEinkommen = zuVersteuerndesEinkommen

            var i:Int = schwellwerte.size
            while (i != 0){
                i--
                schwellwert = schwellwerte[i].toDouble()
                if (nochZuVersteuerndesEinkommen < schwellwert) continue
                grenzsteuersatz = grenzsteuersaetze[i+1]

                steuer += (nochZuVersteuerndesEinkommen-schwellwert)*grenzsteuersatz
                //Log.d(TAG, "Lohnsteuer Monate 1-12    temp: $nochZuVersteuerndesEinkommen, schwellwert: $schwellwert, grenzsteuersatz: $grenzsteuersatz, steuer: $steuer")
                nochZuVersteuerndesEinkommen = schwellwert
            }
            return steuer
        }

        var zvE = 12*(bruttoMonat - svMonat) - werbungskostenpauschale //zu versteuerndes Einkommen

        zvE = round(100*zvE)/100
        //Log.d(TAG, "Lohnsteuer Monate 1-12    zvE:$zvE")

        steuerLaufendeBezuegeVielleichtNegativ = berechneLohnsteuerLaufendeBezuege(zvE) - verkehrsAbsetzbetrag
        steuerLaufendeBezuege = max(0.0, steuerLaufendeBezuegeVielleichtNegativ)

        steuerMonat = steuerLaufendeBezuege/12
        steuerMonatGerundet = round(100*steuerMonat)/100

        //>>für Marginalstundenlohn<<
        var zvEUngerundet = 12*(bruttoMonat - svMonatUngerundet)
        steuerLaufendeBezuegeVielleichtNegativUngerundet = berechneLohnsteuerLaufendeBezuege(zvEUngerundet) - verkehrsAbsetzbetrag

        steuerLaufendeBezuegeUngerundet = max(0.0, steuerLaufendeBezuegeVielleichtNegativUngerundet)

        steuerMonatUngerundet = steuerLaufendeBezuegeUngerundet/12
    }

    private fun lohnsteuerAnmerkungenRueckerstattung(anmerkungenAusgeben:Boolean=true){
        /*
         In bestimmten Fällen kann es bei niedrigen Einkünften vorkommen, dass es zu einer Steuergutschrift in Form einer SV-Rückerstattung kommt.
         https://transparenzportal.gv.at/tdb/tp/leistung/1053727.html
         Besteht Anspruch auf den Verkehrsabsetzbetrag und ergibt sich eine Einkommensteuer unter null, werden 50 % der Sozialversicherungsbeiträge,
         höchstens aber 400 Euro jährlich rückerstattet (SV-Rückerstattung), bei Anspruch auf ein Pendlerpauschale höchstens 500 Euro.
         Bei Anspruch auf den Zuschlag zum Verkehrsabsetzbetrag erhöht sich auch die maximale SV-Rückerstattung um bis zu 400 Euro.
         ArbeitnehmerInnen und PensionistInnen, die so wenig verdienen, dass sie keine Lohnsteuer zahlen (unter ca. € 1.300  brutto/Monat), können sich vom Finanzamt die Negativsteuer zurückholen. Voraussetzung ist, dass sie Sozialversicherung zahlen.
        */

        val zuschlagZumVerkehrsabsetzbetrag:Double
        when {
            bruttoJahr <= 16000  ->  zuschlagZumVerkehrsabsetzbetrag = 650.0
            bruttoJahr >= 24500  ->  zuschlagZumVerkehrsabsetzbetrag = 0.0
            else -> zuschlagZumVerkehrsabsetzbetrag = 650.0*(1-(bruttoJahr - 16000)/(24500-16000))
        }

        rueckzahlungen = 0.0
        if (zuschlagZumVerkehrsabsetzbetrag > 0 + ERR && steuerLaufendeBezuege > 0 + ERR){
            var rueckzahlung = min(zuschlagZumVerkehrsabsetzbetrag, steuerLaufendeBezuege)
            rueckzahlung = round(100*rueckzahlung)/100
            if (anmerkungenAusgeben){
                val rueckzahlungString = String.format("%,.2f", rueckzahlung)
                tvAnmerkungen.text = tvAnmerkungen.text.toString() + "Im Folgejahr werden $rueckzahlungString EUR rückerstattet. Das liegt daran, dass der 'Zuschlag zum Verkehrsabsetzbetrag' erst bei der Arbeitnehmerveranlagung berücksichtigt wird. "
            }
            rueckzahlungen += rueckzahlung
        }

        if (steuerLaufendeBezuegeVielleichtNegativ - zuschlagZumVerkehrsabsetzbetrag < 0 - ERR && svJahr>0 + ERR) {
            var negativeEinkommenssteuer = min (400.0, svJahr/2)
            negativeEinkommenssteuer = round(100*negativeEinkommenssteuer)/100
            if (anmerkungenAusgeben) {
                val negativeEinkommenssteuerString = String.format("%,.2f", negativeEinkommenssteuer)
                tvAnmerkungen.text = tvAnmerkungen.text.toString() + "Im Folgejahr werden zusätzlich $negativeEinkommenssteuerString EUR rückerstattet. Der Grund: Die Lohnsteuer für die reguläten Monate ist durch den (Zuschlag zum) Verkehrsabsetzbetrag rechnerisch negativ. Daher wird durch die Arbeitnehmerveranlagung Geld zurückgezahlt. Diese besteht aus 50% der SV-Beiträge, max. jedoch 400 EUR. (siehe 'SV-Rückerstattung für Geringverdiener' bzw. 'negative Einkommenssteuer'). "
            }
            rueckzahlungen += negativeEinkommenssteuer
        }
        if (rueckzahlungen>0){
            tvAnmerkungen.text = tvAnmerkungen.text.toString() + "Die 'Antragslose Arbeitnehmerveranlagung' wird in der zweiten Jahreshälfte des Folgejahres automatisch durchgeführt.\n\n"
        }
    }

    private fun lohnsteuerSonstigeBezuege(anmerkungenAusgeben:Boolean=true){
        /*
        Einkommenssteuergesetz §67 Sonstige Bezüge
        https://www.ris.bka.gv.at/NormDokument.wxe?Abfrage=Bundesnormen&Gesetzesnummer=10004570&Paragraf=67

        Sonderregelung: Ist für die sonst. Bezüge insgesamt das zvE maximal 2100€, so entfällt die LSt
        Soweit die sonstigen Bezüge gemäß Abs. 1 mehr als das Jahressechstel oder nach Abzug der in Abs. 12 genannten Beträge mehr als 83 333 Euro betragen, sind diese übersteigenden Bezüge im Auszahlungsmonat nach Abs. 10 zu besteuern.

        Wenn das Jahressechstel 2.100 EUR nicht übersteigt (Freigrenze), bleibt die Sonderzahlung – soweit sie den Freibetrag von 620 EUR überschreitet – zwar steuerpflichtig,
         die 6 % Lohnsteuer werden aber (vorläufig) nicht eingehoben. Wenn in einem späteren Monat bei Auszahlung einer weiteren Sonderzahlung das Jahressechstel von 2.100 EUR überschritten wird,
         ist sowohl diese aktuelle Sonderzahlung als auch die vorhergehende(n), bei denen die Versteuerung unterblieben ist, (nach) zu versteuern.
        */

        var zvE13 = bruttoMonat - sv13
        zvE13 = round(100*zvE13)/100

        var zvESonstigeBezuege = 2*bruttoMonat - (sv13 + sv14)
        zvESonstigeBezuege = round(100*zvESonstigeBezuege)/100

        if (anmerkungenAusgeben){
            when {
                (620 < zvESonstigeBezuege && zvESonstigeBezuege <= 2100) -> tvAnmerkungen.text=tvAnmerkungen.text.toString() + "Die Sonstigen Bezüge (13. + 14. Gehalt) abzgl. SV sind zusammen kleiner gleich 2100€ (Freigrenze). Gemäß §67 (2) EStG unterbleibt daher deren Besteuerung.\n\n"
                (zvESonstigeBezuege > 83333) -> tvAnmerkungen.text=tvAnmerkungen.text.toString() + "Die Sonstigen Bezüge (13. + 14. Gehalt) abzgl. SV betragen mehr als 83 333 Euro. Gemäß §67 (2) EStG werden die den Betrag übersteigenden Bezüge im Auszahlungsmonat besteuert. Deshalb zeigt die Tabelle falsche Werte an.\n\n"
                else -> {}
            }
        }

        fun berechneLohnsteuerSonstigeBezuege(zuVersteuerndesEinkommen:Double, runden:Boolean=true):Double {
            var zvE = zuVersteuerndesEinkommen
            val intervalle = listOf(620.0, 24380.0, 25000.0, 33333.0, POSITIVE_INFINITY) //hier anders definiert, keine Schwellwerte
            val grenzsteuersaetze = listOf(0.0, 0.06, 0.27, 0.3575, 0.0)

            var steuer = 0.0
            var i = 0
            var grenzsteuersatz:Double
            var tempZuVersteuern:Double
            while(zvE != 0.0){
                grenzsteuersatz = grenzsteuersaetze[i]

                tempZuVersteuern = min(intervalle[i], zvE)
                if (runden) tempZuVersteuern = round(100*tempZuVersteuern)/100

                zvE -= tempZuVersteuern
                if (runden) zvE = round(zvE*100)/100

                steuer += grenzsteuersatz * tempZuVersteuern
                //Log.d(TAG, "Lohnsteuer Monate 13 14      zvE=$zvE, tempZuVersteuern=$tempZuVersteuern, grenzsteuersatz=$grenzsteuersatz")
                i++
            }
            return steuer
        }

        if(zvESonstigeBezuege<=2100){
            steuer13 = 0.0
            steuer14 = 0.0
        } else {
            steuer13 = berechneLohnsteuerSonstigeBezuege(zvE13)
            steuer14 = berechneLohnsteuerSonstigeBezuege(zvESonstigeBezuege) - steuer13
        }
        steuerJahr = steuerLaufendeBezuege + steuer13 + steuer14

        steuerMonatGerundet = round(100*steuerMonat)/100
        steuer13Gerundet = round(100*steuer13)/100
        steuer14Gerundet = round(100*steuer14)/100
        steuerJahr = round(100*steuerJahr)/100

        //>>für Marginallohn<<
        zvE13 = bruttoMonat - sv13Ungerundet

        zvESonstigeBezuege = 2*bruttoMonat - (sv13Ungerundet + sv14Ungerundet)

        if(zvESonstigeBezuege<=2100){
            steuer13Ungerundet = 0.0
            steuer14Ungerundet = 0.0
        } else {
            steuer13Ungerundet = berechneLohnsteuerSonstigeBezuege(zvE13, runden = false)
            steuer14Ungerundet = berechneLohnsteuerSonstigeBezuege(zvESonstigeBezuege, runden = false) - steuer13Ungerundet
        }
        steuerJahrUngerundet = steuerLaufendeBezuegeUngerundet + steuer13Ungerundet + steuer14Ungerundet

    }

    private fun netto(){
        //Nettolohn
        nettoMonat = bruttoMonat - svMonat - steuerMonatGerundet
        netto13 = bruttoMonat - sv13 - steuer13Gerundet
        netto14 = bruttoMonat - sv14 - steuer14Gerundet
        nettoJahr = bruttoJahr - svJahr - steuerJahr

        nettoMonat = round(100*nettoMonat)/100
        netto13 = round(100*netto13)/100
        netto14 = round(100*netto14)/100
        nettoJahr = round(100*nettoJahr)/100

        //>>Marginallohn<<
        nettoMonatUngerundet = bruttoMonat - svMonatUngerundet - steuerMonatUngerundet
        netto13Ungerundet = bruttoMonat - sv13Ungerundet - steuer13Ungerundet
        netto14Ungerundet = bruttoMonat - sv14Ungerundet - steuer14Ungerundet
        nettoJahrUngerundet = bruttoJahr - svJahrUngerundet - steuerJahrUngerundet
    }

    private fun tabelleFuellen(){
        tvBruttoMonat.text = String.format("%,.2f", bruttoMonat)
        tvBrutto13.text = String.format("%,.2f", bruttoMonat)
        tvBrutto14.text = String.format("%,.2f", bruttoMonat)
        tvBruttoJahr.text = String.format("%,.2f", bruttoJahr)

        tvSVMonat.text = String.format("%,.2f", svMonat)
        tvSV13.text = String.format("%,.2f", sv13)
        tvSV14.text = String.format("%,.2f", sv14)
        tvSVJahr.text = String.format("%,.2f", svJahr)

        tvSteuerMonat.text = String.format("%,.2f", steuerMonatGerundet)
        tvSteuer13.text = String.format("%,.2f", steuer13Gerundet)
        tvSteuer14.text = String.format("%,.2f", steuer14Gerundet)
        tvSteuerJahr.text = String.format("%,.2f", steuerJahr)

        tvNettoMonat.text = String.format("%,.2f", nettoMonat)
        tvNetto13.text = String.format("%,.2f", netto13)
        tvNetto14.text = String.format("%,.2f", netto14)
        tvNettoJahr.text = String.format("%,.2f", nettoJahr)
    }

    private fun stundenlohn(){
        if (stundenEdit.text.toString().isEmpty() ||
            stundenEdit.text.toString() == "." ||
            stundenEdit.text.toString().toDouble() == 0.0)
        {
            val input=stundenEdit.text.toString()
            //Log.d(TAG, "No positive number in second box. Input is: $input")
            tableLayoutStundenlohn.visibility = View.GONE
            return
        }
        tableLayoutStundenlohn.visibility = View.VISIBLE

        val stundenProWoche = stundenEdit.text.toString().toDouble()

        val urlaubstage = 25
        val feiertage2022 = 10  //Feiertage 2022 in Wien, die nicht auf ein Wochenende fallen
        val wochentageProJahr2022 = 260 //2022

        val arbeitstage = wochentageProJahr2022 - urlaubstage - feiertage2022

        val stundenProJahr = arbeitstage*stundenProWoche/5
        val stundeBrutto = bruttoJahr/stundenProJahr
        val stundeNetto = nettoJahr/stundenProJahr

        val stundeBruttoUngerundet = stundeBrutto

        stundeBruttoGerundet = round(100*stundeBrutto)/100
        stundeNettoGerundet = round(100*stundeNetto)/100

        tvStundeBrutto.text = String.format("%,.2f", stundeBrutto)
        tvStundeNetto.text = String.format("%,.2f", stundeNetto)



        //Marginalstundenlohn:

        if ( (260-10-25)*stundenEdit.text.toString().toDouble()/5 < 1){ //-> weniger als eine Stunde im Jahr gearbeitet
            tableRowMarginal.visibility = TableRow.GONE
            return
        }
        tableRowMarginal.visibility = TableRow.VISIBLE

        val nettoJahrNormalUngerundet = nettoJahrUngerundet //errechnet wird nun der Mehrverdienst (netto), wenn in dem Jahr eine Stunde (insgesamt, nicht wöchentlich) mehr gearbeitet wird
        //val rundungsfaktor = 30// um von Rundungseffekten zu abstrahieren
        bruttoJahr -= stundeBrutto
        bruttoMonat = bruttoJahr/14
        sozialversicherungsabgaben()
        lohnsteuerLaufendeBezuege()
        lohnsteuerSonstigeBezuege(anmerkungenAusgeben = false)
        netto()


        stundeMarginalGerundet = (nettoJahrNormalUngerundet - nettoJahrUngerundet)
        stundeMarginalGerundet = round(100*stundeMarginalGerundet)/100
        val stundeMarginalGerundetString = String.format("%,.2f", stundeMarginalGerundet)
        tvStundeMarginal.text = stundeMarginalGerundetString
        val stundenProJahrString:String
        if (stundenProJahr == round(stundenProJahr)){
            stundenProJahrString = String.format("%,d", round(stundenProJahr).toInt())
        } else {
            stundenProJahrString = String.format("%,.2f", stundenProJahr)
        }
        var ausgabe = "Mit $stundeMarginalGerundetString EUR (Netto) wird die letzte der $stundenProJahrString Arbeitsstunden des Jahres vergütet."

        /*
        if (rueckzahlungen>0) {
            val rueckzahlungenNormal = rueckzahlungen
            lohnsteuerAnmerkungenRueckerstattung(anmerkungenAusgeben = false)
            val rueckzahlungen1hWeniger = rueckzahlungen
            var stundeMarginalMitRueckzahlungen = (nettoJahrNormal + rueckzahlungenNormal) - (nettoJahr + rueckzahlungen1hWeniger)
            stundeMarginalMitRueckzahlungen = round(100 * stundeMarginalMitRueckzahlungen) / 100
            if(stundeMarginalGerundet < stundeMarginalMitRueckzahlungen){
                val stundeMarginalMitRueckzahlungenString = String.format("%,.2f", stundeMarginalMitRueckzahlungen)
                ausgabe += " Berücksichtigen wir die weiter unten beschriebenen Rückzahlung(en), so wurde sie mit $stundeMarginalMitRueckzahlungenString EUR vergütet."
            }
        }*/
        tvAnmerkungen.text = ausgabe + "\n\n" + tvAnmerkungen.text
    }
}
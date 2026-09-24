package com.ebingo.backend.user.constant;

import com.ebingo.backend.user.dto.BotUserBulkCreateDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in pool of Ethiopian names used when bulk-creating bot users.
 * A random entry is picked for each bot when the request does not supply
 * its own {@code names} list. The pool intentionally mixes Amharic and
 * Oromo names, male and female, so large batches (up to 500 bots) still
 * look varied.
 *
 * The pool contains {@link #CURATED} hand-picked entries (with nicknames)
 * plus generated first-name x last-name combinations, for a total of
 * 500+ unique names.
 */
public final class BotNamePool {

    private BotNamePool() {
    }

    /**
     * Hand-picked names with nicknames (includes the original robot seed names).
     */
    private static final List<BotUserBulkCreateDto.BotName> CURATED = List.of(
            new BotUserBulkCreateDto.BotName("Daniel", "Kebede", "Dan_k"),
            new BotUserBulkCreateDto.BotName("Abel", "Seyum", "Abela"),
            new BotUserBulkCreateDto.BotName("Gemechu", "Debela", "Gemechu"),
            new BotUserBulkCreateDto.BotName("Fraol", "Kuma", "Fraol"),
            new BotUserBulkCreateDto.BotName("Meaza", "Hunachew", "Meazi"),
            new BotUserBulkCreateDto.BotName("Mikiyas", "Abebe", "Miki"),
            new BotUserBulkCreateDto.BotName("Jaalala", "Bekele", "Jaalala"),
            new BotUserBulkCreateDto.BotName("Henok", "Yohannes", "Heni"),
            new BotUserBulkCreateDto.BotName("Rediet", "Fikre", "Redi"),
            new BotUserBulkCreateDto.BotName("Daba", "Kebede", "Daba"),
            new BotUserBulkCreateDto.BotName("Rahel", "Gebru", "Rahela"),
            new BotUserBulkCreateDto.BotName("Samuel", "Bekele", "Sammy"),
            new BotUserBulkCreateDto.BotName("Lensa", "Merga", "Lensi"),
            new BotUserBulkCreateDto.BotName("Biruk", "Tadele", "Biru"),
            new BotUserBulkCreateDto.BotName("Yonatan", "Solomon", "Yoni"),
            new BotUserBulkCreateDto.BotName("Hundee", "Fufa", "Hundee"),
            new BotUserBulkCreateDto.BotName("Abebe", "Tesfaye", "Abe"),
            new BotUserBulkCreateDto.BotName("Kebede", "Alemu", "Kebe"),
            new BotUserBulkCreateDto.BotName("Tesfaye", "Girma", "Tesfa"),
            new BotUserBulkCreateDto.BotName("Girma", "Haile", "Giri"),
            new BotUserBulkCreateDto.BotName("Tadesse", "Worku", "Tadi"),
            new BotUserBulkCreateDto.BotName("Haile", "Desta", "Hailu"),
            new BotUserBulkCreateDto.BotName("Mengistu", "Assefa", "Mengi"),
            new BotUserBulkCreateDto.BotName("Alemayehu", "Kassa", "Alema"),
            new BotUserBulkCreateDto.BotName("Yilma", "Negash", "Yilma"),
            new BotUserBulkCreateDto.BotName("Assefa", "Lemma", "Asefa"),
            new BotUserBulkCreateDto.BotName("Desta", "Mamo", "Desta"),
            new BotUserBulkCreateDto.BotName("Fikadu", "Tekle", "Fika"),
            new BotUserBulkCreateDto.BotName("Getachew", "Wolde", "Geta"),
            new BotUserBulkCreateDto.BotName("Hailu", "Yimer", "Hailu"),
            new BotUserBulkCreateDto.BotName("Mulugeta", "Zewdie", "Mulu"),
            new BotUserBulkCreateDto.BotName("Negash", "Arega", "Nega"),
            new BotUserBulkCreateDto.BotName("Sisay", "Berhe", "Sisi"),
            new BotUserBulkCreateDto.BotName("Tamrat", "Gemeda", "Tamra"),
            new BotUserBulkCreateDto.BotName("Wendimu", "Tolera", "Wendi"),
            new BotUserBulkCreateDto.BotName("Zewdu", "Wako", "Zewdu"),
            new BotUserBulkCreateDto.BotName("Birhanu", "Yadeta", "Biru"),
            new BotUserBulkCreateDto.BotName("Dawit", "Abara", "Dawit"),
            new BotUserBulkCreateDto.BotName("Ephrem", "Benti", "Ephi"),
            new BotUserBulkCreateDto.BotName("Fasil", "Chala", "Fasil"),
            new BotUserBulkCreateDto.BotName("Gebre", "Dibaba", "Gebre"),
            new BotUserBulkCreateDto.BotName("Kassahun", "Etana", "Kassa"),
            new BotUserBulkCreateDto.BotName("Mesfin", "Gurmu", "Mesfi"),
            new BotUserBulkCreateDto.BotName("Neway", "Hordofa", "Neway"),
            new BotUserBulkCreateDto.BotName("Tewodros", "Ibsa", "Tewod"),
            new BotUserBulkCreateDto.BotName("Yonas", "Jilo", "Yoni"),
            new BotUserBulkCreateDto.BotName("Zelalem", "Kenea", "Zela"),
            new BotUserBulkCreateDto.BotName("Addisu", "Leta", "Addis"),
            new BotUserBulkCreateDto.BotName("Belay", "Megersa", "Belay"),
            new BotUserBulkCreateDto.BotName("Chala", "Nigatu", "Chala"),
            new BotUserBulkCreateDto.BotName("Demeke", "Olana", "Deme"),
            new BotUserBulkCreateDto.BotName("Eshetu", "Regassa", "Esh"),
            new BotUserBulkCreateDto.BotName("Fekadu", "Sime", "Feka"),
            new BotUserBulkCreateDto.BotName("Gashaw", "Tola", "Gash"),
            new BotUserBulkCreateDto.BotName("Habtamu", "Umeta", "Habta"),
            new BotUserBulkCreateDto.BotName("Jemal", "Wario", "Jemal"),
            new BotUserBulkCreateDto.BotName("Kifle", "Yadesa", "Kifle"),
            new BotUserBulkCreateDto.BotName("Lulseged", "Zewde", "Lul"),
            new BotUserBulkCreateDto.BotName("Morkos", "Alemayehu", "Morkos"),
            new BotUserBulkCreateDto.BotName("Nigussie", "Bekele", "Nigu"),
            new BotUserBulkCreateDto.BotName("Robel", "Desta", "Robi"),
            new BotUserBulkCreateDto.BotName("Solomon", "Fikadu", "Soli"),
            new BotUserBulkCreateDto.BotName("Tilahun", "Getachew", "Tila"),
            new BotUserBulkCreateDto.BotName("Wondimu", "Hailu", "Wondi"),
            new BotUserBulkCreateDto.BotName("Yared", "Mulugeta", "Yared"),
            new BotUserBulkCreateDto.BotName("Zeleke", "Negash", "Zele"),
            new BotUserBulkCreateDto.BotName("Abiy", "Sisay", "Abiy"),
            new BotUserBulkCreateDto.BotName("Biniam", "Tamrat", "Bini"),
            new BotUserBulkCreateDto.BotName("Dagmawi", "Wendimu", "Dagi"),
            new BotUserBulkCreateDto.BotName("Eyob", "Zewdu", "Eyob"),
            new BotUserBulkCreateDto.BotName("Fitsum", "Birhanu", "Fiti"),
            new BotUserBulkCreateDto.BotName("Girmay", "Dawit", "Girmay"),
            new BotUserBulkCreateDto.BotName("Haymanot", "Ephrem", "Hayman"),
            new BotUserBulkCreateDto.BotName("Kirubel", "Fasil", "Kiru"),
            new BotUserBulkCreateDto.BotName("Leul", "Gebre", "Leul"),
            new BotUserBulkCreateDto.BotName("Mintesinot", "Kassahun", "Minte"),
            new BotUserBulkCreateDto.BotName("Nahom", "Mesfin", "Nahom"),
            new BotUserBulkCreateDto.BotName("Surafel", "Neway", "Sura"),
            new BotUserBulkCreateDto.BotName("Temesgen", "Tewodros", "Teme"),
            new BotUserBulkCreateDto.BotName("Wubshet", "Yonas", "Wub"),
            new BotUserBulkCreateDto.BotName("Yohannes", "Zelalem", "Yohannes"),
            new BotUserBulkCreateDto.BotName("Zerihun", "Addisu", "Zeri"),
            new BotUserBulkCreateDto.BotName("Almaz", "Belay", "Almaz"),
            new BotUserBulkCreateDto.BotName("Bethlehem", "Chala", "Beti"),
            new BotUserBulkCreateDto.BotName("Chaltu", "Demeke", "Chaltu"),
            new BotUserBulkCreateDto.BotName("Debritu", "Eshetu", "Debri"),
            new BotUserBulkCreateDto.BotName("Emebet", "Fekadu", "Eme"),
            new BotUserBulkCreateDto.BotName("Frehiwot", "Gashaw", "Fre"),
            new BotUserBulkCreateDto.BotName("Genet", "Habtamu", "Genet"),
            new BotUserBulkCreateDto.BotName("Hiwot", "Jemal", "Hiwot"),
            new BotUserBulkCreateDto.BotName("Kidist", "Kifle", "Kidi"),
            new BotUserBulkCreateDto.BotName("Lemlem", "Lulseged", "Lemlem"),
            new BotUserBulkCreateDto.BotName("Meskerem", "Morkos", "Meski"),
            new BotUserBulkCreateDto.BotName("Nardos", "Nigussie", "Nardi"),
            new BotUserBulkCreateDto.BotName("Selam", "Robel", "Selam"),
            new BotUserBulkCreateDto.BotName("Tigist", "Solomon", "Tigi"),
            new BotUserBulkCreateDto.BotName("Wubit", "Tilahun", "Wubit"),
            new BotUserBulkCreateDto.BotName("Yeshi", "Wondimu", "Yeshi"),
            new BotUserBulkCreateDto.BotName("Zewditu", "Yared", "Zewdi"),
            new BotUserBulkCreateDto.BotName("Aster", "Zeleke", "Aster"),
            new BotUserBulkCreateDto.BotName("Birtukan", "Abiy", "Birtu"),
            new BotUserBulkCreateDto.BotName("Dinknesh", "Biniam", "Dinku"),
            new BotUserBulkCreateDto.BotName("Etalemahu", "Dagmawi", "Etale"),
            new BotUserBulkCreateDto.BotName("Fantu", "Eyob", "Fantu"),
            new BotUserBulkCreateDto.BotName("Gelila", "Fitsum", "Geli"),
            new BotUserBulkCreateDto.BotName("Haregewoin", "Girmay", "Hargo"),
            new BotUserBulkCreateDto.BotName("Konjit", "Haymanot", "Konji"),
            new BotUserBulkCreateDto.BotName("Lulit", "Kirubel", "Lulit"),
            new BotUserBulkCreateDto.BotName("Meron", "Leul", "Meron"),
            new BotUserBulkCreateDto.BotName("Nigist", "Mintesinot", "Nigist"),
            new BotUserBulkCreateDto.BotName("Roman", "Nahom", "Roman"),
            new BotUserBulkCreateDto.BotName("Senait", "Surafel", "Sena"),
            new BotUserBulkCreateDto.BotName("Tirunesh", "Temesgen", "Tiru"),
            new BotUserBulkCreateDto.BotName("Woinshet", "Wubshet", "Woini"),
            new BotUserBulkCreateDto.BotName("Yordanos", "Yohannes", "Yordi"),
            new BotUserBulkCreateDto.BotName("Zenebech", "Zerihun", "Zene"),
            new BotUserBulkCreateDto.BotName("Atsede", "Almaz", "Atsede"),
            new BotUserBulkCreateDto.BotName("Bezawit", "Bethlehem", "Beza"),
            new BotUserBulkCreateDto.BotName("Derartu", "Chaltu", "Derartu"),
            new BotUserBulkCreateDto.BotName("Elsabet", "Debritu", "Elsa"),
            new BotUserBulkCreateDto.BotName("Feven", "Emebet", "Feven"),
            new BotUserBulkCreateDto.BotName("Genzebe", "Frehiwot", "Genze"),
            new BotUserBulkCreateDto.BotName("Helen", "Genet", "Helen"),
            new BotUserBulkCreateDto.BotName("Kalkidan", "Hiwot", "Kalki"),
            new BotUserBulkCreateDto.BotName("Mahlet", "Kidist", "Mahi"),
            new BotUserBulkCreateDto.BotName("Mihret", "Lemlem", "Mihret"),
            new BotUserBulkCreateDto.BotName("Saron", "Meskerem", "Saron"),
            new BotUserBulkCreateDto.BotName("Tsion", "Nardos", "Tsion"),
            new BotUserBulkCreateDto.BotName("Wrodi", "Selam", "Wrodi"),
            new BotUserBulkCreateDto.BotName("Yemisrach", "Tigist", "Yemi"),
            new BotUserBulkCreateDto.BotName("Bontu", "Daba", "Bontu"),
            new BotUserBulkCreateDto.BotName("Dureti", "Gemechu", "Dureti"),
            new BotUserBulkCreateDto.BotName("Galane", "Hundee", "Gala"),
            new BotUserBulkCreateDto.BotName("Ifa", "Jaalala", "Ifa"),
            new BotUserBulkCreateDto.BotName("Jilo", "Lensa", "Jilo"),
            new BotUserBulkCreateDto.BotName("Kena", "Fraol", "Kena"),
            new BotUserBulkCreateDto.BotName("Lalise", "Bontu", "Lali"),
            new BotUserBulkCreateDto.BotName("Mardasa", "Dureti", "Mardi"),
            new BotUserBulkCreateDto.BotName("Nagawo", "Galane", "Naga"),
            new BotUserBulkCreateDto.BotName("Oli", "Ifa", "Oli"),
            new BotUserBulkCreateDto.BotName("Qabale", "Jilo", "Qabale"),
            new BotUserBulkCreateDto.BotName("Roba", "Kena", "Roba"),
            new BotUserBulkCreateDto.BotName("Sime", "Lalise", "Sime"),
            new BotUserBulkCreateDto.BotName("Tolosa", "Mardasa", "Tolo"),
            new BotUserBulkCreateDto.BotName("Urji", "Nagawo", "Urji"),
            new BotUserBulkCreateDto.BotName("Walabuma", "Oli", "Wala"),
            new BotUserBulkCreateDto.BotName("Yanet", "Qabale", "Yanet"),
            new BotUserBulkCreateDto.BotName("Zalalaka", "Roba", "Zala"),
            new BotUserBulkCreateDto.BotName("Boke", "Sime", "Boke"),
            new BotUserBulkCreateDto.BotName("Dhaba", "Tolosa", "Dhaba"),
            new BotUserBulkCreateDto.BotName("Ebisa", "Urji", "Ebisa"),
            new BotUserBulkCreateDto.BotName("Falmata", "Walabuma", "Falmi"),
            new BotUserBulkCreateDto.BotName("Gudina", "Yanet", "Gudina"),
            new BotUserBulkCreateDto.BotName("Hawi", "Zalalaka", "Hawi"),
            new BotUserBulkCreateDto.BotName("Ibsa", "Boke", "Ibsa"),
            new BotUserBulkCreateDto.BotName("Jaldesa", "Dhaba", "Jaldi")
    );

    /**
     * Extra first names combined with {@link #EXTRA_LAST_NAMES} to push the
     * pool past 500 entries. Nickname defaults to the first name.
     */
    private static final String[] EXTRA_FIRST_NAMES = {
            "Amanuel", "Anbessa", "Ashenafi", "Ayele", "Balcha", "Belete",
            "Beshir", "Bulcha", "Dagne", "Dejene", "Demissie", "Desalegn",
            "Dugassa", "Endale", "Fisseha", "Gadisa", "Gebeyehu", "Getnet",
            "Gobena", "Gudeta", "Abaynesh", "Alemitu", "Ayantu", "Belaynesh"
    };

    private static final String[] EXTRA_LAST_NAMES = {
            "Abate", "Aga", "Amente", "Asfaw", "Ayana", "Bedada", "Beriso",
            "Beshah", "Biratu", "Bultum", "Cheru", "Degefa", "Dinberu",
            "Edessa", "Fayisa"
    };

    /**
     * Full pool: curated entries + every extra first/last combination.
     * Size = 156 + (24 x 15) = 516 names.
     */
    public static final List<BotUserBulkCreateDto.BotName> NAMES = buildPool();

    private static List<BotUserBulkCreateDto.BotName> buildPool() {
        List<BotUserBulkCreateDto.BotName> pool =
                new ArrayList<>(CURATED.size() + EXTRA_FIRST_NAMES.length * EXTRA_LAST_NAMES.length);
        pool.addAll(CURATED);
        for (String first : EXTRA_FIRST_NAMES) {
            for (String last : EXTRA_LAST_NAMES) {
                pool.add(new BotUserBulkCreateDto.BotName(first, last, first));
            }
        }
        return List.copyOf(pool);
    }
}

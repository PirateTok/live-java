package com.piratetok.live.gifts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapping of TikTok Live gift names from English to German.
 * Note: TikTok displays most gift names in English even in the German app.
 * These translations are used for German UI display.
 */
public final class GiftNames {

    public static final Map<String, String> EN_TO_DE;

    static {
        Map<String, String> m = new HashMap<>();

        // 1-Coin gifts
        m.put("Rose", "Rose");
        m.put("TikTok", "TikTok-Symbol");
        m.put("GG", "GG");
        m.put("Heart", "Herz");
        m.put("Blue Heart", "Blaues Herz");
        m.put("Flame heart", "Flammenherz");
        m.put("Heart Puff", "Herz-Puff");
        m.put("Thumbs Up", "Daumen hoch");
        m.put("Ice Cream Cone", "Eistüte");
        m.put("Cake Slice", "Kuchenstück");
        m.put("Glow Stick", "Leuchtstab");
        m.put("Love you", "Ich liebe dich");
        m.put("Love you so much", "Ich liebe dich so sehr");
        m.put("Birthday Cake", "Geburtstagskuchen");
        m.put("Heart Me", "Teamherz");
        m.put("Congratulations", "Glückwunsch");
        m.put("So Cute", "So süß");
        m.put("You're awesome", "Du bist toll");
        m.put("Pop", "Pop");
        m.put("Creeper", "Creeper");
        m.put("Wink wink", "Zwinkern");
        m.put("Freestyle", "Freestyle");
        m.put("Oldies", "Oldies");
        m.put("Power hug", "Kraftumarmung");
        m.put("Squirrel", "Eichhörnchen");
        m.put("Chilli Pepper", "Chillischote");
        m.put("Tulip", "Tulpe");
        m.put("Music Album", "Musikalbum");
        m.put("Go Popular", "Beliebt werden");
        m.put("Club Cheers", "Club-Jubel");
        m.put("Wink Charm", "Zauber-Zwinkern");

        // 2-Coin gifts
        m.put("Team Bracelet", "Team-Armband");

        // 5-Coin gifts
        m.put("Finger Heart", "Fingerherz");
        m.put("Overreact", "Überreagieren");
        m.put("Name shoutout", "Namentlicher Gruß");
        m.put("Pomegranate", "Granatapfel");
        m.put("Embroidered Heart", "Gesticktes Herz");

        // 10–99-Coin gifts
        m.put("Rosa", "Rosa");
        m.put("Friendship Necklace", "Freundschaftskette");
        m.put("Chocolate", "Schokolade");
        m.put("Heart Gaze", "Herzblick");
        m.put("Perfume", "Parfüm");
        m.put("Doughnut", "Donut");
        m.put("Butterfly", "Schmetterling");
        m.put("Paper Crane", "Papierkranich");
        m.put("Little Crown", "Kleine Krone");
        m.put("Cap", "Kappe");
        m.put("Hat and Mustache", "Hut und Schnurrbart");
        m.put("Love Painting", "Liebesgemälde");
        m.put("Bubble Gum", "Kaugummi");
        m.put("Cupid's Bow", "Amors Bogen");
        m.put("Confetti", "Konfetti");
        m.put("Hand Hearts", "Handherzen");
        m.put("Panda", "Panda");
        m.put("Sushi Set", "Sushi-Set");
        m.put("Coffee Magic", "Kaffee-Magie");

        // 100–499-Coin gifts
        m.put("Hearts", "Herzen");
        m.put("Sunglasses", "Sonnenbrillen");
        m.put("Corgi", "Corgi");
        m.put("Boxing Gloves", "Boxhandschuhe");
        m.put("Duck", "Ente");
        m.put("Singing Magic", "Singmagie");
        m.put("Balloon Crown", "Ballonkrone");
        m.put("Flower Headband", "Blumen-Stirnband");
        m.put("Rose Hand", "Rosenhand");
        m.put("Night Star", "Nachtstern");
        m.put("Twinkling Star", "Funkelnder Stern");
        m.put("Love Rain", "Liebesregen");
        m.put("Floating Octopus", "Schwebender Oktopus");
        m.put("Party Pony", "Party-Pony");
        m.put("Gold Medal", "Goldmedaille");
        m.put("Magic Genie", "Zaubergeist");
        m.put("Rose Bear", "Rosenbär");
        m.put("Candy Bouquet", "Süßigkeitenstrauß");
        m.put("Forest Elf", "Waldelfe");
        m.put("Surfing Penguin", "Surfender Pinguin");
        m.put("Koala Love", "Koala-Liebe");

        // 500-Coin gifts
        m.put("Money Gun", "Geldpistole");
        m.put("XXXL Flowers", "XXXL-Blumen");
        m.put("Flower Show", "Blumenshow");
        m.put("Heart Guitar", "Herzgitarre");
        m.put("Mystery Box", "Mysterienbox");
        m.put("VR Goggles", "VR-Brille");
        m.put("DJ Glasses", "DJ-Brille");
        m.put("Dragon Crown", "Drachenkrone");
        m.put("Racing Helmet", "Rennhelm");

        // 1.000-Coin gifts
        m.put("Galaxy", "Galaxie");
        m.put("Swan", "Schwan");
        m.put("Join Butterflies", "Schmetterlinge vereinen");
        m.put("Fireworks", "Feuerwerk");
        m.put("Colorful Wings", "Bunte Flügel");
        m.put("Love Flight", "Liebesflug");
        m.put("Train", "Zug");
        m.put("Travel with You", "Ich reise mit dir");
        m.put("Fairy Wings", "Feenflügel");
        m.put("Sparkle Dance", "Funkel-Tanz");
        m.put("Shiny air balloon", "Glänzender Heißluftballon");

        // 2.000–5.000-Coin gifts
        m.put("Silver Sports Car", "Silber-Sportwagen");
        m.put("Flamingo Groove", "Flamingo-Groove");
        m.put("Party Bus", "Partybus");
        m.put("Meteor Shower", "Meteorschauer");
        m.put("Motorcycle", "Motorrad");
        m.put("Pink Dream", "Pinker Traum");
        m.put("Rhythmic Bear", "Rhythmischer Bär");
        m.put("Ocelot Chase", "Ozelot-Jagd");
        m.put("Wild Mic", "Wildes Mikrofon");

        // 5.000–15.000-Coin gifts
        m.put("Private Jet", "Privatjet");
        m.put("Signature Jet", "Signatur-Jet");
        m.put("Diamond Gun", "Diamantpistole");
        m.put("Unicorn Fantasy", "Einhorn-Fantasie");
        m.put("Wolf", "Wolf");
        m.put("Fiery Dragon", "Feuriger Drache");
        m.put("Sports Car", "Sportwagen");
        m.put("Happy Party", "Fröhliche Party");
        m.put("Majestic Hearts", "Majestätische Herzen");
        m.put("Star Throne", "Sternenthron");
        m.put("Interstellar", "Interstellar");
        m.put("Sunset Speedway", "Sonnenuntergangs-Rennstrecke");

        // 15.000–30.000-Coin gifts
        m.put("Whale Diving", "Tauchender Wal");
        m.put("Falcon", "Falke");
        m.put("Dragon Flame", "Drachen-Flamme");
        m.put("Phoenix", "Phönix");
        m.put("Leon the Kitten", "Leon das Kätzchen");
        m.put("Lili the Leopard", "Lili der Leopard");
        m.put("Leopard", "Leopard");
        m.put("Snow Leopard", "Schneeleopard");
        m.put("Stallion", "Hengst");
        m.put("Sneaky Jockey", "Heimlicher Jockey");
        m.put("Amusement Park", "Vergnügungspark");
        m.put("Castle Fantasy", "Schloss-Fantasie");
        m.put("Fly Love", "Fliegende Liebe");
        m.put("Paris", "Paris");
        m.put("Golden Gallop", "Goldener Galopp");
        m.put("Undersea Kingdom", "Unterwasserkönigreich");
        m.put("Future City", "Zukunftsstadt");
        m.put("Crystal Heart", "Kristallherz");

        // 30.000–45.000-Coin gifts (top tier)
        m.put("Lion", "Löwe");
        m.put("Zeus", "Zeus");
        m.put("Leon and Lili", "Leon und Lili");
        m.put("Leon and Lion", "Leon und Löwe");
        m.put("Pegasus", "Pegasus");
        m.put("Thunder Falcon", "Donnerfalke");
        m.put("Fire Phoenix", "Feuer-Phönix");
        m.put("Red Lightning", "Roter Blitz");
        m.put("TikTok Shuttle", "TikTok-Shuttle");
        m.put("Cyber Roar", "Cyber-Gebrüll");
        m.put("TikTok Stars", "TikTok-Sterne");
        m.put("TikTok Universe", "TikTok-Universum");
        m.put("TikTok Universe+", "TikTok-Universum+");
        m.put("Legend Marcellus", "Legende Marcellus");
        m.put("Julius the Champion", "Julius der Champion");

        EN_TO_DE = Collections.unmodifiableMap(m);
    }

    /**
     * Thrown when a TikTok gift name has no entry in {@link #EN_TO_DE} (add the mapping and redeploy).
     */
    public static final class UnknownGiftMappingException extends IllegalStateException {
        public UnknownGiftMappingException(String englishName) {
            super("No German mapping for TikTok gift name: \"" + englishName + "\"");
        }
    }

    /**
     * Returns the German name for a given English gift name.
     * Falls back to the English name if no translation exists.
     */
    public static String translate(String englishName) {
        return EN_TO_DE.getOrDefault(englishName, englishName);
    }

    /**
     * Returns the German UI label for an English gift name from TikTok.
     *
     * @throws IllegalArgumentException if {@code englishName} is null or blank
     * @throws UnknownGiftMappingException if there is no entry in {@link #EN_TO_DE}
     */
    public static String translateRequired(String englishName) {
        if (englishName == null || englishName.isBlank()) {
            throw new IllegalArgumentException("TikTok gift name is null or blank");
        }
        String key = englishName.strip();
        String de = EN_TO_DE.get(key);
        if (de == null) {
            throw new UnknownGiftMappingException(key);
        }
        return de;
    }

    private GiftNames() {}
}

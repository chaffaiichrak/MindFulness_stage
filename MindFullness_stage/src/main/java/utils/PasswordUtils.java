package utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║           PasswordUtils — MindAura Security                  ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Hachage et vérification des mots de passe avec BCrypt.      ║
 * ║                                                              ║
 * ║  BCrypt :                                                    ║
 * ║   • Algorithme de référence pour le stockage de mots passe  ║
 * ║   • Intègre un sel aléatoire automatiquement                ║
 * ║   • Résistant aux attaques par dictionnaire et rainbow table ║
 * ║   • Le facteur de travail (cost) rend le brute-force lent   ║
 * ║                                                              ║
 * ║  Dépendance Maven à ajouter dans pom.xml :                  ║
 * ║   <dependency>                                               ║
 * ║     <groupId>org.mindrot</groupId>                           ║
 * ║     <artifactId>jbcrypt</artifactId>                         ║
 * ║     <version>0.4</version>                                   ║
 * ║   </dependency>                                              ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class PasswordUtils {

    /**
     * Facteur de coût BCrypt (2^cost itérations).
     * 12 = bon équilibre sécurité / performance (~300ms sur CPU moderne).
     * Ne pas descendre en dessous de 10 en production.
     */
    private static final int BCRYPT_COST = 12;

    private PasswordUtils() {}


    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty())
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide.");
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Vérifie si un mot de passe en clair correspond à un hash BCrypt.
     *
     * @param plainPassword  mot de passe saisi par l'utilisateur
     * @param hashedPassword hash stocké en base de données
     * @return               true si le mot de passe est correct
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        if (!hashedPassword.startsWith("$2")) return false; // hash BCrypt invalide
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            // Hash malformé ou autre erreur
            return false;
        }
    }

    /**
     * Indique si une chaîne est déjà un hash BCrypt (commence par "$2a$" ou "$2b$").
     * Utile pour la migration : éviter de re-hacher un mot de passe déjà haché.
     *
     * @param value  chaîne à tester
     * @return       true si c'est déjà un hash BCrypt
     */
    public static boolean isHashed(String value) {
        return value != null && value.startsWith("$2");
    }
}

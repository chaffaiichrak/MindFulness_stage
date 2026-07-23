package enums;

public enum StatusReservation {
    EN_ATTENTE("En Attente"),
    CONFIRMEE("Confirmee"),
    ANNULEE("Annulee"),
    TERMINEE("Terminee");

    private final String libelle;

    StatusReservation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static StatusReservation fromString(String text) {
        if (text == null) return EN_ATTENTE;

        text = text.trim().toUpperCase();

        try {
            return valueOf(text);
        } catch (IllegalArgumentException e) {
            return EN_ATTENTE; // Valeur par défaut
        }
    }

    @Override
    public String toString() {
        return libelle;
    }
}

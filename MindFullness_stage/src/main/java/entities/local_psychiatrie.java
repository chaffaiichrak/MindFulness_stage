package entities;

public class local_psychiatrie {
    private int id_local;
    private String nom_local;
    private String adresse_local;
    private int capacite_local;
    private int telephone_local;
    private String disponibilite_local;
    private String ville_local;
    private String image_url;
    private String description_local;
    private String type_local;


    public local_psychiatrie() {
    }

    public local_psychiatrie(int id_local, String nom_local, String adresse_local, int capacite_local, int telephone_local, String disponibilite_local, String ville_local, String image_url, String description_local, String type_local) {
        this.id_local = id_local;
        this.nom_local = nom_local;
        this.adresse_local = adresse_local;
        this.capacite_local = capacite_local;
        this.telephone_local = telephone_local;
        this.disponibilite_local = disponibilite_local;
        this.ville_local = ville_local;
        this.image_url = image_url;
        this.description_local = description_local;
        this.type_local = type_local;
    }

    public int getId_local() {
        return id_local;
    }

    public void setId_local(int id_local) {
        this.id_local = id_local;
    }

    public String getNom_local() {
        return nom_local;
    }

    public void setNom_local(String nom_local) {
        this.nom_local = nom_local;
    }

    public String getAdresse_local() {
        return adresse_local;
    }

    public void setAdresse_local(String adresse_local) {
        this.adresse_local = adresse_local;
    }

    public int getCapacite_local() {
        return capacite_local;
    }

    public void setCapacite_local(int capacite_local) {
        this.capacite_local = capacite_local;
    }

    public int getTelephone_local() {
        return telephone_local;
    }

    public void setTelephone_local(int telephone_local) {
        this.telephone_local = telephone_local;
    }


    public String getDisponibilite_local() {
        return disponibilite_local;
    }

    public void setDiscription_local(String discription_local) {
        this.description_local = discription_local;
    }

    public String getVille_local() {
        return ville_local;
    }

    public void setVille_local(String ville_local) {
        this.ville_local = ville_local;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getDescription_local() {
        return description_local;
    }

    public void setDisponibilite_local(String disponibilite_local) {
        this.disponibilite_local = disponibilite_local;
    }
    public String getType_local() {return type_local;}
    public void setType_local(String type_local) {this.type_local = type_local;}

    @Override
    public String toString() {
        return "local_psychiatrie{" +
                "id_local=" + id_local +
                ", nom_local='" + nom_local + '\'' +
                ", adresse_local='" + adresse_local + '\'' +
                ", ville_local='" + ville_local + '\'' +
                ", description_local='" + description_local + '\'' +
                ", capacite_local='" + capacite_local + '\'' +
                ", type_local='" + type_local + '\'' +
                ", telephone_local=" + telephone_local +
                ", disponibilite_local='" + disponibilite_local + '\'' +
                ", imageURL='" + image_url + '\'' +
                '}';
    }
}


package entities;

import enums.MotifReservation;
import enums.StatusReservation;


import java.util.Date;

public class reservation_local {
    private int id_reservation;
    private int id_local;
    private Date date_reservation;
    private Date heure_debut_reservation;
    private Date heure_fin_reservation;
    private StatusReservation status_reservation;
    private MotifReservation motif_reservation;
    private int prix_reservation;
    private String nom_cl;
    private String prenom_cl;

    public reservation_local() {
    }
    public reservation_local(int id_reservation, int id_local, Date date_reservation,
                             Date heure_debut_reservation, Date heure_fin_reservation, StatusReservation status_reservation,
                             MotifReservation motif_reservation, int prix_reservation, String nom_cl, String prenom_cl) {
        this.id_reservation = id_reservation;
        this.id_local = id_local;
        this.date_reservation = date_reservation;
        this.heure_debut_reservation = heure_debut_reservation;
        this.heure_fin_reservation = heure_fin_reservation;
        this.status_reservation = status_reservation;
        this.motif_reservation = motif_reservation;
        this.prix_reservation = prix_reservation;
        this.nom_cl = nom_cl;
        this.prenom_cl = prenom_cl;
    }
    public int getId_reservation() {return id_reservation;}
    public void setId_reservation(int id_reservation) {this.id_reservation = id_reservation;}
    public int getId_local() {return id_local;}
    public void setId_local(int id_local) {this.id_local = id_local;}
    public Date getDate_reservation() {return date_reservation;}
    public void setDate_reservation(Date date_reservation) {this.date_reservation = date_reservation;}

    public Date getHeure_debut_reservation() {
        return heure_debut_reservation;
    }

    public void setHeure_debut_reservation(Date heure_debut_reservation) {
        this.heure_debut_reservation = heure_debut_reservation;
    }
    public Date getHeure_fin_reservation() {
        return heure_fin_reservation;
    }
    public void setHeure_fin_reservation(Date heure_fin_reservation) {
        this.heure_fin_reservation = heure_fin_reservation;
    }
    public StatusReservation getStatus_reservation() {
        return status_reservation;
    }
    public void setStatus_reservation(StatusReservation status_reservation) {
        this.status_reservation = status_reservation;
    }
    public MotifReservation getMotif_reservation() {
        return motif_reservation;
    }
    public void setMotif_reservation(MotifReservation motif_reservation) {
        this.motif_reservation = motif_reservation;
    }
    public int getPrix_reservation() {
        return prix_reservation;
    }
    public void setPrix_reservation(int prix_reservation) {
        this.prix_reservation = prix_reservation;
    }
    public String getNom_cl() {return nom_cl;}
    public void setNom_cl(String nom_cl) {this.nom_cl = nom_cl;}
    public String getPrenom_cl() {return prenom_cl;}

    public void setPrenom_cl(String prenom_cl) {
        this.prenom_cl = prenom_cl;
    }
}

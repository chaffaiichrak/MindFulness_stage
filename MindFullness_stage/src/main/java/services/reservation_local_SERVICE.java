package services;

import entities.reservation_local;
import enums.MotifReservation;
import utils.MyDataBase;
import enums.StatusReservation;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class reservation_local_SERVICE implements ICRUD<reservation_local> {
    private Connection conx;
    private Statement stm;
    private PreparedStatement pstm;
    public reservation_local_SERVICE() {
        conx = MyDataBase.getInstance().getConx();
    }
    @Override
    public void add(reservation_local reservationLocal) throws SQLException {
        String req = "INSERT INTO reservation_local"
                + "(id_local, date_reservation, heure_debut_reservation, "
                + "heure_fin_reservation, status_reservation, motif_reservation, prix_reservation, "
                + "nom_cl, prenom_cl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        pstm = conx.prepareStatement(req);
        pstm.setInt(1, reservationLocal.getId_local());
        pstm.setDate(2, new java.sql.Date(reservationLocal.getDate_reservation().getTime()));
        pstm.setTimestamp(3, new java.sql.Timestamp(reservationLocal.getHeure_debut_reservation().getTime()));
        pstm.setTimestamp(4, new java.sql.Timestamp(reservationLocal.getHeure_fin_reservation().getTime()));
        pstm.setString(5, reservationLocal.getStatus_reservation().name());
        pstm.setString(6, reservationLocal.getMotif_reservation().name());
        pstm.setInt(7, reservationLocal.getPrix_reservation());
        pstm.setString(8, reservationLocal.getNom_cl() != null ? reservationLocal.getNom_cl() : "");
        pstm.setString(9, reservationLocal.getPrenom_cl() != null ? reservationLocal.getPrenom_cl() : "");

        pstm.executeUpdate();
        System.out.println("Réservation ajoutée avec succès");
    }

    @Override
    public void modifier(reservation_local reservationLocal) throws SQLException {
        System.out.println("DEBUG - ID à modifier: " + reservationLocal.getId_reservation());

        String req = "UPDATE reservation_local SET "
                // + "id_utilisateur = ?, "
                + "id_local= ?, "
                + "date_reservation = ?, "
                + "heure_debut_reservation = ?, "
                + "heure_fin_reservation = ?, "
                + "status_reservation = ?, "
                + "motif_reservation = ?, "
                + "prix_reservation = ?, "
                + "nom_cl = ?, "
                + "prenom_cl = ? "
                + "WHERE id_reservation = ?";

        pstm = conx.prepareStatement(req);
        //pstm.setInt(1, reservationLocal.getId_utilisateur());
        pstm.setInt(1, reservationLocal.getId_local());
        pstm.setDate(2, new java.sql.Date(reservationLocal.getDate_reservation().getTime()));
        pstm.setTimestamp(3, new java.sql.Timestamp(reservationLocal.getHeure_debut_reservation().getTime()));
        pstm.setTimestamp(4, new java.sql.Timestamp(reservationLocal.getHeure_fin_reservation().getTime()));
        pstm.setString(5, reservationLocal.getStatus_reservation().name());
        pstm.setString(6, reservationLocal.getMotif_reservation().name());
        pstm.setInt(7, reservationLocal.getPrix_reservation());

        if (reservationLocal.getNom_cl() != null) {
            pstm.setString(8, reservationLocal.getNom_cl());
        } else {
            pstm.setNull(8, Types.VARCHAR);
        }

        if (reservationLocal.getPrenom_cl() != null) {
            pstm.setString(9, reservationLocal.getPrenom_cl());
        } else {
            pstm.setNull(9, Types.VARCHAR);
        }

        pstm.setInt(10, reservationLocal.getId_reservation());

        int rowsAffected = pstm.executeUpdate();

        System.out.println("DEBUG - Lignes affectées: " + rowsAffected);

        if (rowsAffected > 0) {
            System.out.println("Réservation modifiée avec succès");
        } else {
            System.out.println("Aucune réservation trouvée avec l'ID: " + reservationLocal.getId_reservation());
        }

    }

    @Override
    public void delete(reservation_local reservationLocal) throws SQLException {
        String req = "DELETE FROM reservation_local WHERE id_reservation = ?";

        pstm = conx.prepareStatement(req);
        pstm.setInt(1, reservationLocal.getId_reservation());

        int rowsAffected = pstm.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("Réservation supprimée avec succès");
        } else {
            System.out.println("Aucune réservation trouvée avec l'ID: " + reservationLocal.getId_reservation());
        }
    }

    @Override
    public List<reservation_local> afficherList() throws SQLException {
        List<reservation_local> liste = new ArrayList<>();
        String req = "SELECT id_reservation,  id_local, date_reservation, "
                + "heure_debut_reservation, heure_fin_reservation, status_reservation, "
                + "motif_reservation, prix_reservation, nom_cl, prenom_cl "
                + "FROM reservation_local";
        stm = conx.createStatement();
        ResultSet rs = stm.executeQuery(req);

        while (rs.next()) {
            liste.add(new reservation_local(
                            rs.getInt("id_reservation"),
                            rs.getInt("id_local"),
                            rs.getDate("date_reservation"),
                            rs.getTimestamp("heure_debut_reservation"),
                            rs.getTimestamp("heure_fin_reservation"),
                            StatusReservation.fromString(rs.getString("Status_reservation")),
                            MotifReservation.fromString(rs.getString("motif_reservation")),
                            rs.getInt("prix_reservation"),
                            rs.getString("nom_cl"),
                            rs.getString("prenom_cl")
                    )
            );
        }
        return liste;
    }
}
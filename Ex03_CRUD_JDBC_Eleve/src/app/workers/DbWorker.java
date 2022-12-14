package app.workers;

import app.beans.Personne;
import app.exceptions.MyDBException;
import app.helpers.DateTimeLib;
import app.helpers.SystemLib;
import com.mysql.cj.jdbc.result.ResultSetFactory;

import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DbWorker implements DbWorkerItf {

    private Connection dbConnexion;
    private int index = 0;

    /**
     * Constructeur du worker
     */
    public DbWorker() {
    }

    @Override
    public void connecterBdMySQL(String nomDB) throws MyDBException {
        final String url_remote = "jdbc:mysql://localhost:3306/" + nomDB;
        //final String url_remote = "jdbc:mysql://172.23.85.187:3306/" + nomDB;
        final String user = "root";
        final String password = "emf123";

        System.out.println("url:" + url_remote);
        try {
            dbConnexion = DriverManager.getConnection(url_remote, user, password);
        } catch (SQLException ex) {
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }

    @Override
    public void connecterBdHSQLDB(String nomDB) throws MyDBException {
        final String url = "jdbc:hsqldb:file:" + nomDB + ";shutdown=true";
        final String user = "SA";
        final String password = "";
        System.out.println("url:" + url);
        try {
            dbConnexion = DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }

    @Override
    public void connecterBdAccess(String nomDB) throws MyDBException {
        final String url = "jdbc:ucanaccess://" + nomDB;
        System.out.println("url=" + url);
        try {
            dbConnexion = DriverManager.getConnection(url);
        } catch (SQLException ex) {
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }

    @Override
    public void deconnecter() throws MyDBException {
        try {
            if (dbConnexion != null) {
                dbConnexion.close();
            }
        } catch (SQLException ex) {
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }

    public List<Personne> lirePersonnes() throws MyDBException {
        List<Personne> listePersonnes = new ArrayList<>();
        try {
            if(dbConnexion != null) {
                Statement stmt = dbConnexion.createStatement();
                ResultSet res = stmt.executeQuery("SELECT * FROM t_personne");
                while (res.next()) {
                    listePersonnes.add(
                            new Personne(
                                    res.getInt("PK_PERS"),
                                    res.getString("Nom"),
                                    res.getString("Prenom"),
                                    new java.util.Date(res.getDate("Date_Naissance").getTime()),
                                    res.getInt("No_rue"),
                                    res.getString("Rue"),
                                    res.getInt("NPA"),
                                    res.getString("Ville"),
                                    res.getByte("Actif") == 1,
                                    res.getDouble("Salaire"),
                                    new java.util.Date(res.getDate("date_modif").getTime())
                            )
                    );
                }
            }
        } catch (SQLException e) {
            throw new MyDBException("lirePersonnes",e.getMessage());
        }
        return listePersonnes;
    }


    @Override
    public void creer(Personne p) throws MyDBException {
        try {
            PreparedStatement stmt = dbConnexion.prepareStatement(
                    "INSERT INTO t_personne VALUES (DEFAULT,?,?,?,?,?,?,?,?,?,?,DEFAULT);"
            );
            stmt.setString(1, p.getPrenom());
            stmt.setString(2, p.getNom());
            stmt.setDate(3, new Date(p.getDateNaissance().getTime()));
            stmt.setInt(4,p.getNoRue());
            stmt.setString(5,p.getRue());
            stmt.setInt(6,p.getNpa());
            stmt.setString(7,p.getLocalite());
            stmt.setByte(8, (byte) (p.isActif() ? 1 : 0));
            stmt.setDouble(9,p.getSalaire());
            stmt.setTimestamp(10,new Timestamp((new java.util.Date()).getTime()));
            if(!(stmt.executeUpdate() > 0)) throw new MyDBException("creer","Pas de cr??ation");
            Statement st = dbConnexion.createStatement();
            ResultSet res = st.executeQuery("SELECT PK_PERS FROM t_personne ORDER BY PK_PERS DESC LIMIT 1");
            p.setPkPers(res.next() ? res.getInt("PK_PERS") : -1);
        } catch (SQLException e) {
            throw new MyDBException("creer",e.getMessage());
        }
    }

    @Override
    public Personne lire(int pk) throws MyDBException {
        Statement stmt = null;
        try {
            stmt = dbConnexion.createStatement();
            ResultSet res =
                    stmt.executeQuery("SELECT * FROM t_personne " +
                                            "WHERE PK_PERS = "+pk+";");
            return res.next() ? new Personne(
                    res.getInt("PK_PERS"),
                    res.getString("Nom"),
                    res.getString("Prenom"),
                    new java.util.Date(res.getDate("Date_Naissance").getTime()),
                    res.getInt("No_rue"),
                    res.getString("Rue"),
                    res.getInt("NPA"),
                    res.getString("Ville"),
                    res.getByte("Actif") == 1,
                    res.getDouble("Salaire"),
                    new java.util.Date(res.getDate("date_modif").getTime())
            ) :  null;
        } catch (SQLException e) {
            throw new MyDBException("lire",e.getMessage());
        }
    }

    @Override
    public void modifier(Personne p) throws MyDBException {
        try {
            PreparedStatement stmt = dbConnexion.prepareStatement(
                    "UPDATE t_personne a" +
                            " INNER JOIN t_personne b ON b.PK_PERS = ? SET  "
                    +  " a.Prenom = ?,"
                    +  " a.Nom = ?,"
                    +  " a.Date_naissance = ?,"
                    +  " a.No_rue = ?,"
                    +  " a.Rue = ?,"
                    +  " a.NPA = ?,"
                    +  " a.Ville = ?,"
                    +  " a.Actif = ?,"
                    +  " a.Salaire = ?,"
                    +  " a.date_modif = ?,"
                    +  " a.no_modif = b.no_modif+1"
                    +  " WHERE a.PK_PERS = ?"
            );
            stmt.setDouble(1,p.getPkPers());
            stmt.setString(2,p.getPrenom());
            stmt.setString(3,p.getNom());
            stmt.setDate(4, new Date(p.getDateNaissance().getTime()));
            stmt.setInt(5,p.getNoRue());
            stmt.setString(6,p.getRue());
            stmt.setInt(7,p.getNpa());
            stmt.setString(8,p.getLocalite());
            stmt.setByte(9, (byte) (p.isActif() ? 1 : 0));
            stmt.setDouble(10,p.getSalaire());
            stmt.setTimestamp(11,new Timestamp((new java.util.Date()).getTime()));
            stmt.setDouble(12,p.getPkPers());
            if(!(stmt.executeUpdate() > 0)) throw new MyDBException("modifier","Pas de modification");
        } catch (SQLException e) {
            throw new MyDBException("modifier",e.getMessage());
        }

    }

    @Override
    public void effacer(Personne p) throws MyDBException {
        try {
            PreparedStatement stmt = dbConnexion.prepareStatement(
                    "DELETE FROM t_personne WHERE PK_PERS = ?"
            );
            stmt.setInt(1,p.getPkPers());
            if(!(stmt.executeUpdate() > 0)) throw new MyDBException("effacer","Aucun effacement");
        } catch (SQLException e) {
            throw new MyDBException("effacer",e.getMessage());
        }
    }
}

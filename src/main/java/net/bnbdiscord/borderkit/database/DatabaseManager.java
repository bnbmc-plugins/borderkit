package net.bnbdiscord.borderkit.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class DatabaseManager {
    private Dao<Jurisdiction, Long> jurisdictionDao;

    public DatabaseManager() {
        try {
            JdbcPooledConnectionSource connectionSource = new JdbcPooledConnectionSource("jdbc:sqlite:borderkit.db");

            TableUtils.createTableIfNotExists(connectionSource, Jurisdiction.class);
            jurisdictionDao = DaoManager.createDao(connectionSource, Jurisdiction.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Dao<Jurisdiction, Long> getJurisdictionDao() {
        return jurisdictionDao;
    }
}

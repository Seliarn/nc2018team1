package com.nc.airport.backend.persistence.eav.mutable2query.data.modification;

import com.nc.airport.backend.persistence.eav.Mutable;
import com.nc.airport.backend.persistence.eav.exceptions.DatabaseConnectionException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public abstract class SequenceBuilder {
    protected final Logger LOGGER = LogManager.getLogger(this.getClass());
    protected Connection connection;

    public SequenceBuilder(Connection connection) {
        this.connection = connection;
    }

    public abstract Mutable build(Mutable mutable);

    boolean noSuchElementsInObject(Map map) {
        return map == null || map.size() == 0;
    }

    protected void logSQLError(SQLException e, String inTable, String operation) {
        LOGGER.log(Level.ERROR, e);
        throw new DatabaseConnectionException("Invalid values in mutable for " + operation + " in " + inTable, e);
    }

    public BigInteger getNewObjectId() {
        try (Statement statement = connection.createStatement();
             ResultSet nextVal = statement.executeQuery("SELECT COALESCE(MIN(O1.OBJECT_ID + 1), 1)\n" +
                     "  FROM OBJECTS O1 LEFT JOIN OBJECTS O2 ON O1.OBJECT_ID + 1 = O2.OBJECT_ID\n" +
                     "  WHERE O2.OBJECT_ID IS NULL")) {
            nextVal.next();
            return new BigInteger(nextVal.getString(1));
        } catch (SQLException e) {
            LOGGER.error("Failed to generate new object id ", e);
            throw new DatabaseConnectionException("Failed to fetch vacant objectId", e);
        }
    }
}

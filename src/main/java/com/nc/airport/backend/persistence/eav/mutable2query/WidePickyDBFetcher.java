package com.nc.airport.backend.persistence.eav.mutable2query;

import com.nc.airport.backend.persistence.eav.Mutable;
import com.nc.airport.backend.persistence.eav.exceptions.BadDBRequestException;
import com.nc.airport.backend.persistence.eav.exceptions.DatabaseConnectionException;
import com.nc.airport.backend.persistence.eav.mutable2query.filtering2sorting.FilteringToSortingDescriptor;
import com.nc.airport.backend.persistence.eav.mutable2query.filtering2sorting.filtering.FilterEntity;
import com.nc.airport.backend.persistence.eav.mutable2query.filtering2sorting.paging.PagingDescriptor;
import com.nc.airport.backend.persistence.eav.mutable2query.filtering2sorting.sorting.SortEntity;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;

import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Log4j2
class WidePickyDBFetcher {
    private Connection connection;

    public WidePickyDBFetcher(Connection connection) {
        this.connection = connection;
    }

    List<Mutable> getMutables(List<BigInteger> values,
                              List<BigInteger> dateValues,
                              List<BigInteger> listValues,
                              List<BigInteger> references,
                              int pagingFrom, int pagingTo,
                              List<SortEntity> sortBy,
                              List<FilterEntity> filterBy) {

        List<Mutable> mutables = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet result = null;
        PagingDescriptor paging = new PagingDescriptor();
        FilteringToSortingDescriptor.DescriptorBuilder descBuilder =
                new FilteringToSortingDescriptor.DescriptorBuilder();

        values = ensureNonNullSecurity(values);
        dateValues = ensureNonNullSecurity(dateValues);
        listValues = ensureNonNullSecurity(listValues);
        references = ensureNonNullSecurity(references);

        if (!Collections.isEmpty(filterBy))
            descBuilder.filter(filterBy);
        if (!Collections.isEmpty(sortBy))
            descBuilder.sort(sortBy);

        StringBuilder basicQuery = createSQLQuery(values, dateValues, listValues, references);
        StringBuilder filteredSortedQuery = new StringBuilder("SELECT * FROM (").append(basicQuery).append(") ");
        filteredSortedQuery.append(descBuilder.build().getQueryBuilder());
        String fullQuery = paging.getPaging(filteredSortedQuery, pagingFrom, pagingTo);

        try {
            log.log(Level.INFO, "Executing sequence:\n" + fullQuery);
            statement = connection.prepareStatement(fullQuery);
            result = resultMultipleMutables(statement, values, dateValues, listValues, references, filterBy);
            while (result.next()) {
                Mutable mutable = new Mutable();
                pullGeneralInfo(result, mutable);
                pullAttributes(result, mutable, values, dateValues, listValues, references);
                mutables.add(mutable);
            }
        }catch (SQLException e) {
            log.error(e);
            throw new DatabaseConnectionException("Could not open statement", e);
        } finally {
            closeResultSetAndStatement(result, statement);
        }
        return mutables;
    }

    int getCountOfMutables(List<BigInteger> values,
                                  List<BigInteger> dateValues,
                                  List<BigInteger> listValues,
                                  List<BigInteger> references,
                                  List<FilterEntity> filterBy) {

        PreparedStatement statement = null;
        ResultSet result = null;
        FilteringToSortingDescriptor.DescriptorBuilder descBuilder =
                new FilteringToSortingDescriptor.DescriptorBuilder();
        int countOfItems = 0;

        values = ensureNonNullSecurity(values);
        dateValues = ensureNonNullSecurity(dateValues);
        listValues = ensureNonNullSecurity(listValues);
        references = ensureNonNullSecurity(references);

        if (!Collections.isEmpty(filterBy))
            descBuilder.filter(filterBy);

        StringBuilder basicQuery = createSQLQuery(values, dateValues, listValues, references);
        StringBuilder countFilteredSortedQuery = new StringBuilder("SELECT COUNT(*) AS total FROM (").append(basicQuery).append(") ");
        countFilteredSortedQuery.append(descBuilder.build().getQueryBuilder());
        try {
            log.log(Level.INFO, "Executing sequence:\n" + countFilteredSortedQuery);
            statement = connection.prepareStatement(countFilteredSortedQuery.toString());
            result = resultMultipleMutables(statement, values, dateValues, listValues, references, filterBy);
            while (result.next()) {
                countOfItems = result.getInt("total");
            }
        }catch (SQLException e) {
            log.error(e);
            throw new DatabaseConnectionException("Could not open statement", e);
        } finally {
            closeResultSetAndStatement(result, statement);
        }
        return countOfItems;
    }

    List<Mutable> getMutablesByParentId(List<BigInteger> values,
                              List<BigInteger> dateValues,
                              List<BigInteger> listValues,
                              List<BigInteger> references, int pagingFrom, int pagingTo,
                              BigInteger parentId) {
        List<Mutable> mutables = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet result = null;
        PagingDescriptor paging = new PagingDescriptor();

        values = ensureNonNullSecurity(values);
        dateValues = ensureNonNullSecurity(dateValues);
        listValues = ensureNonNullSecurity(listValues);
        references = ensureNonNullSecurity(references);

        StringBuilder basicQuery = createSQLQuery(values, dateValues, listValues, references);
        basicQuery.append("WHERE O.PARENT_ID = ").append(parentId);
        String fullQuery = paging.getPaging(basicQuery, pagingFrom, pagingTo);

        try {
            log.log(Level.INFO, "Executing sequence:\n" + fullQuery);
            statement = connection.prepareStatement(fullQuery);
            result = resultMultipleMutables(statement, values, dateValues, listValues, references, null);
            while (result.next()) {
                Mutable mutable = new Mutable();
                pullGeneralInfo(result, mutable);
                pullAttributes(result, mutable, values, dateValues, listValues, references);
                mutables.add(mutable);
            }
        }catch (SQLException e) {
            log.error(e);
            throw new DatabaseConnectionException("Could not open statement", e);
        } finally {
            closeResultSetAndStatement(result, statement);
        }
        return mutables;

    }

    /*  SELECT * FROM
            ( SELECT a.*, rownum rnum
                FROM
                (SELECT * FROM
                    (
                    SELECT O.OBJECT_ID, O.PARENT_ID, O.OBJECT_TYPE_ID, O.NAME, O.DESCRIPTION,
                        A1.VALUE ATTR51,
                        A2.DATE_VALUE ATTR50,
                        A3.LIST_VALUE_ID ATTR45,
                        A4.REFERENCE ATTR55
                    FROM OBJECTS O
                    JOIN ATTRIBUTES A1
                      ON A1.ATTR_ID = 51 AND A1.OBJECT_ID = O.OBJECT_ID
                    JOIN ATTRIBUTES A2
                      ON A2.ATTR_ID = 50 AND A2.OBJECT_ID = O.OBJECT_ID
                    JOIN ATTRIBUTES A3
                      ON A3.ATTR_ID = 45 AND A3.OBJECT_ID = O.OBJECT_ID
                    JOIN OBJREFERENCE A4
                      ON A4.ATTR_ID = 55 AND A4.OBJECT_ID = O.OBJECT_ID
                    )
                  WHERE (ATTR45 = 1 OR ATTR45 = 2) ORDER BY ATTR50 DESC
                  ) a
                WHERE rownum <= 2)
            WHERE rnum >= 1                                                                                 */
    private StringBuilder createSQLQuery(List<BigInteger> values,
                                         List<BigInteger> dateValues,
                                         List<BigInteger> listValues,
                                         List<BigInteger> references) {
        StringBuilder query = new StringBuilder
                ("  SELECT O.OBJECT_ID, O.PARENT_ID, O.OBJECT_TYPE_ID, O.NAME, O.DESCRIPTION, ");
                transferAttributesSelection(query, values, dateValues, listValues, references);
                query.append(" FROM OBJECTS O ");
                transferAttributesJoin(query, values, dateValues, listValues, references);

        return query;
    }

    private void transferAttributesSelection(StringBuilder transferTo,
                                             List<BigInteger> values,
                                             List<BigInteger> dateValues,
                                             List<BigInteger> listValues,
                                             List<BigInteger> references) {
        int i = 1;
        i = selectEachAttr(values, "VALUE", transferTo, i);
        i = selectEachAttr(dateValues, "DATE_VALUE", transferTo, i);
        i = selectEachAttr(listValues, "LIST_VALUE_ID", transferTo, i);
            selectEachAttr(references, "REFERENCE", transferTo, i);
        transferTo.delete(transferTo.lastIndexOf(","), transferTo.length() - 1);
    }

    private int selectEachAttr(List<BigInteger> attrs,
                               String attrColumnType,
                               StringBuilder transferTo,
                               int i) {

        for (BigInteger attr : attrs) {
            transferTo.append("A").append(i++).append(".").append(attrColumnType)
                    .append(" ATTR").append(attr).append(", ");
        }
        return i;
    }

    private void transferAttributesJoin(StringBuilder transferTo,
                                        List<BigInteger> values,
                                        List<BigInteger> dateValues,
                                        List<BigInteger> listValues,
                                        List<BigInteger> references) {
        int i = 1;
        i = joinEachAttr(values, "ATTRIBUTES", transferTo, i);
        i = joinEachAttr(dateValues, "ATTRIBUTES", transferTo, i);
        i = joinEachAttr(listValues, "ATTRIBUTES", transferTo, i);
            joinEachAttr(references, "OBJREFERENCE", transferTo, i);
    }

    private int joinEachAttr(List<BigInteger> attrs,
                             String attrTableType,
                             StringBuilder transferTo,
                             int i) {

        for (int j = 0; j < attrs.size(); j++) {
            String alias = " A" + (i++);
            transferTo.append(" JOIN ").append(attrTableType).append(alias)
                    .append(" ON").append(alias).append(".ATTR_ID = ?")
                    .append(" AND").append(alias).append(".OBJECT_ID = O.OBJECT_ID ");
        }
        return i;
    }

    private void closeResultSetAndStatement(ResultSet result, Statement statement) {
        if (result != null)
            try {
                result.close();
            } catch (SQLException e) {
                log.error(e);
                throw new DatabaseConnectionException("Could not close result set", e);
            }

        if (statement != null)
            try {
                statement.close();
            } catch (SQLException e) {
                log.error(e);
                throw new DatabaseConnectionException("Could not close statement", e);
            }
    }

    private ResultSet resultMultipleMutables(PreparedStatement statement,
                                             List<BigInteger> values,
                                             List<BigInteger> dateValues,
                                             List<BigInteger> listValues,
                                             List<BigInteger> references,
                                             List<FilterEntity> filters) {

        int indexesBefore = 0;
        setAttributes(statement, values, indexesBefore);

        indexesBefore = values.size();
        setAttributes(statement, dateValues, indexesBefore);

        indexesBefore = indexesBefore + dateValues.size();
        setAttributes(statement, listValues, indexesBefore);

        indexesBefore = indexesBefore + listValues.size();
        setAttributes(statement, references, indexesBefore);

        if (filters != null) {
            indexesBefore = indexesBefore + references.size();
            setFilters(statement, filters, indexesBefore);
        }
        try {
            return statement.executeQuery();
        } catch (SQLException e) {
            log.error(e);
            throw new BadDBRequestException("Error is found after query execution", e);
        }
    }

    private void setAttributes(PreparedStatement statement,
                               Collection<BigInteger> attributesId,
                               int indexesBefore) {

        try {
            Iterator<BigInteger> attrIterator = attributesId.iterator();
            for (int i = indexesBefore + 1; i <= attributesId.size() + indexesBefore; i++) {
                BigInteger attrId = attrIterator.next();
                statement.setObject(i, attrId);
            }
        } catch (SQLException e) {
            log.error(e);
            throw new BadDBRequestException("Could not set given attributes", e);
        }
    }

    private void setFilters(PreparedStatement statement,
                            List<FilterEntity> filters,
                            int indexesBefore) {

        try {
            Iterator<FilterEntity> attrIterator = filters.iterator();
            for (int i = indexesBefore + 1; i <= getFiltersGlobalSize(filters) + indexesBefore; i++) {
                Set<Object> filterValues = attrIterator.next().getValues();

                Iterator<Object> filterValuesIterator = filterValues.iterator();
                int j;
                for (j = i; j < filterValues.size() + i; j++) {
                    Object filterVal = filterValuesIterator.next();
                    statement.setObject(j, filterVal);
                }
                i = j - 1;
            }
        } catch (SQLException e) {
            log.error(e);
            throw new BadDBRequestException("Could not set given filters", e);
        }
    }

    private int getFiltersGlobalSize(List<FilterEntity> filters) {
        int globalSize = 0;
        for (FilterEntity entity : filters) {
            globalSize += entity.getValues().size();
        }
        return globalSize;
    }

    private void pullGeneralInfo(ResultSet result, Mutable mutable) {
        try {
            mutable.setObjectId(applyBigInt(1, result));
            mutable.setParentId(applyBigInt(2, result));
            mutable.setObjectTypeId(applyBigInt(3, result));
            mutable.setObjectName(result.getString(4));
            mutable.setObjectDescription(result.getString(5));
        } catch (SQLException e) {
            log.error(e);
            throw new BadDBRequestException("Couldn`t pull general object information", e);
        }
    }

    private void pullAttributes(ResultSet result,
                                Mutable mutable,
                                List<BigInteger> values,
                                List<BigInteger> dateValues,
                                List<BigInteger> listValues,
                                List<BigInteger> references) {
        try {
            int indexesBefore = 5; //general object information
            pullValues(result, mutable, values, indexesBefore);

            indexesBefore = indexesBefore + values.size();
            pullDateValues(result, mutable, dateValues, indexesBefore);

            indexesBefore = indexesBefore + dateValues.size();
            pullListValues(result, mutable, listValues, indexesBefore);

            indexesBefore = indexesBefore + listValues.size();
            pullReferences(result, mutable, references, indexesBefore);
        } catch (SQLException e) {
            log.error(e);
            throw new DatabaseConnectionException("Couldn`t pull given attributes", e);
        }
    }

    //fixme try to make next 4 methods less hardcoded
    private void pullValues(ResultSet result,
                            Mutable mutable,
                            List<BigInteger> valuesIn,
                            int indexesBefore) throws SQLException {

        Map<BigInteger, String> valuesOut = new HashMap<>();
        Iterator<BigInteger> attrIterator = valuesIn.iterator();
        for (int i = indexesBefore + 1; i <= valuesIn.size() + indexesBefore; i++) {
            BigInteger attrId = attrIterator.next();
            valuesOut.put(attrId, result.getString(i));
        }
        mutable.setValues(valuesOut);
    }

    private void pullDateValues(ResultSet result,
                                Mutable mutable,
                                List<BigInteger> dateValuesIn,
                                int indexesBefore) throws SQLException {

        Map<BigInteger, LocalDateTime> dateValuesOut = new HashMap<>();
        Iterator<BigInteger> attrIterator = dateValuesIn.iterator();
        for (int i = indexesBefore + 1; i <= dateValuesIn.size() + indexesBefore; i++) {
            BigInteger attrId = attrIterator.next();
            dateValuesOut.put(attrId, result.getTimestamp(i).toLocalDateTime());
        }
        mutable.setDateValues(dateValuesOut);
    }

    private void pullListValues(ResultSet result,
                                Mutable mutable,
                                List<BigInteger> listValuesIn,
                                int indexesBefore) throws SQLException {

        Map<BigInteger, BigInteger> listValuesOut = new HashMap<>();
        Iterator<BigInteger> attrIterator = listValuesIn.iterator();
        for (int i = indexesBefore + 1; i <= listValuesIn.size() + indexesBefore; i++) {
            BigInteger attrId = attrIterator.next();
            listValuesOut.put(attrId, applyBigInt(i, result));
        }
        mutable.setListValues(listValuesOut);
    }

    private void pullReferences(ResultSet result,
                                Mutable mutable,
                                List<BigInteger> referencesIn,
                                int indexesBefore) throws SQLException {

        Map<BigInteger, BigInteger> referencesOut = new HashMap<>();
        Iterator<BigInteger> attrIterator = referencesIn.iterator();
        for (int i = indexesBefore + 1; i <= referencesIn.size() + indexesBefore; i++) {
            BigInteger attrId = attrIterator.next();
            referencesOut.put(attrId, applyBigInt(i, result));
        }
        mutable.setReferences(referencesOut);
    }

    private BigInteger applyBigInt(String bigInt) {
        return bigInt == null ? null : new BigInteger(bigInt);
    }

    private BigInteger applyBigInt(int columnNumber, ResultSet result) throws SQLException {
        return applyBigInt(result.getString(columnNumber));
    }

    private List<BigInteger> ensureNonNullSecurity(List<BigInteger> list) {
        if (list == null){
            return new ArrayList<>();
        } else return list;
    }
}
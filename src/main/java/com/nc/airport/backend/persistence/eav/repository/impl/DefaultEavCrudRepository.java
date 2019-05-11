package com.nc.airport.backend.persistence.eav.repository.impl;

import com.nc.airport.backend.model.BaseEntity;
import com.nc.airport.backend.persistence.eav.Mutable;
import com.nc.airport.backend.persistence.eav.entity2mutable.Entity2Mutable;
import com.nc.airport.backend.persistence.eav.entity2mutable.util.ReflectionHelper;
import com.nc.airport.backend.persistence.eav.exceptions.InvalidDeleteException;
import com.nc.airport.backend.persistence.eav.mutable2query.Mutable2Query;
import com.nc.airport.backend.persistence.eav.mutable2query.filtering2sorting.filtering.FilterEntity;
import com.nc.airport.backend.persistence.eav.mutable2query.filtering2sorting.sorting.SortEntity;
import com.nc.airport.backend.persistence.eav.repository.EavCrudRepository;
import com.nc.airport.backend.persistence.eav.repository.Page;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Repository
public class DefaultEavCrudRepository<T extends BaseEntity> implements EavCrudRepository<T> {
    private Mutable2Query m2db;
    private Entity2Mutable e2m;

    @Autowired
    public DefaultEavCrudRepository(Mutable2Query m2q, Entity2Mutable e2m) {
        this.e2m = e2m;
        this.m2db = m2q;
    }

    @Override
    public <S extends T> S update(S entity) {
        Mutable mutable = e2m.convertEntityToMutable(entity);
        S updatedEntity;

        Mutable updatedMutable = m2db.sqlUpdate(mutable);
        updatedEntity = (S) e2m.convertMutableToEntity(updatedMutable, entity.getClass());
        log.info("Updated. Got an updated entity back : {}", updatedEntity);

        return updatedEntity;
    }

    @Override
    public <S extends T> S insert(S entity) {
        S insertedEntity = update(entity);
        log.info("Inserted. Got an inserted entity back : {}", insertedEntity);

        return insertedEntity;
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
//             TODO MAKE A METHOD THAT SAVES ALL ENTITIES ON DB PART
        List<S> updatedEntities = new ArrayList<>();
        for (S entity : entities) {
            if (entity != null)
                updatedEntities.add(this.update(entity));
        }

        return updatedEntities;
    }

    @Override
    public Optional<T> findById(BigInteger objectId, Class<T> entityClass) {
        checkNull(objectId);
        checkNull(entityClass);

        Mutable optMutable = m2db.getSingleMutable(objectId, ReflectionHelper.getAttributeIds(entityClass));
        T searchedEntity = null;
        if (optMutable != null) {
            searchedEntity = e2m.convertMutableToEntity(optMutable, entityClass);
        }
        return Optional.ofNullable(searchedEntity);
    }

    @Override
//             TODO NAMING, ALSO THINK IF THIS METHOD IS REALLY NEEDED
    public List<T> findAll(Class<T> entityClass, Iterable<BigInteger> objectIds) {
        checkNull(entityClass);

        List<T> entities = new ArrayList<>();
        for (BigInteger objectId : objectIds) {
            Optional<T> result = findById(objectId, entityClass);
            result.ifPresent(entities::add);
        }
        return entities;
    }

    @Override
    public List<T> findSlice(@NotNull Class<T> entityClass, Page page) {
        checkNull(entityClass);

        List<Mutable> mutables;
        mutables = m2db.getMutablesFromDB(ReflectionHelper.getObjTypeId(entityClass),
                ReflectionHelper.getAttributeIds(entityClass),
                page.getFirstRow(),
                page.getLastRow());

        List<T> entities = new ArrayList<>();
        for (Mutable mutable : mutables) {
            entities.add(e2m.convertMutableToEntity(mutable, entityClass));
        }
        return entities;
    }

    @Override
    public List<T> findSliceOfChildren(@NotNull BigInteger parentId, @NotNull Class<T> childClass, Page page) {
        checkNull(childClass);
        checkNull(parentId);

        List<Mutable> mutables;
        mutables = m2db.getMutablesFromDBByParentId(ReflectionHelper.getValueFieldIds(childClass),
                ReflectionHelper.getDateFieldIds(childClass),
                ReflectionHelper.getListFieldIds(childClass),
                ReflectionHelper.getReferenceFieldIds(childClass),
                page.getFirstRow(),
                page.getLastRow(),
                parentId,
                ReflectionHelper.getObjTypeId(childClass));

        List<T> entities = new ArrayList<>();
        for (Mutable mutable : mutables) {
            entities.add(e2m.convertMutableToEntity(mutable, childClass));
        }
        return entities;
    }

    @Override
    public List<T> findSlice(@NotNull Class<T> entityClass, Page page, List<SortEntity> sortBy, List<FilterEntity> filterBy) {
        checkNull(entityClass);

        List<Mutable> mutables;
        mutables = m2db.getMutablesFromDB(
                ReflectionHelper.getValueFieldIds(entityClass),
                ReflectionHelper.getDateFieldIds(entityClass),
                ReflectionHelper.getListFieldIds(entityClass),
                ReflectionHelper.getReferenceFieldIds(entityClass),
                page.getFirstRow(),
                page.getLastRow(),
                sortBy,
                filterBy);

        List<T> entities = new ArrayList<>();
        for (Mutable mutable : mutables) {
            entities.add(e2m.convertMutableToEntity(mutable, entityClass));
        }
        return entities;
    }

    @Override
    public List<T> findSliceOfChildren(@NotNull BigInteger parentId, @NotNull Class<T> childClass, Page page, List<SortEntity> sortBy, List<FilterEntity> filterBy) {
        checkNull(childClass);
        checkNull(parentId);

        List<Mutable> mutables;
        mutables = m2db.getMutablesFromDBByParentId(ReflectionHelper.getValueFieldIds(childClass),
                ReflectionHelper.getDateFieldIds(childClass),
                ReflectionHelper.getListFieldIds(childClass),
                ReflectionHelper.getReferenceFieldIds(childClass),
                page.getFirstRow(),
                page.getLastRow(),
                parentId,
                ReflectionHelper.getObjTypeId(childClass),
                sortBy,
                filterBy);

        List<T> entities = new ArrayList<>();
        for (Mutable mutable : mutables) {
            entities.add(e2m.convertMutableToEntity(mutable, childClass));
        }
        return entities;
    }

    @Override
    public List<T> findSliceOfReference(@NotNull BigInteger objectId, @NotNull Class<T> entityClass) {
        checkNull(entityClass);

        List<Mutable> mutables;
        mutables = m2db.getMutablesByReference(
                ReflectionHelper.getValueFieldIds(entityClass),
                ReflectionHelper.getDateFieldIds(entityClass),
                ReflectionHelper.getListFieldIds(entityClass),
                ReflectionHelper.getReferenceFieldIds(entityClass), objectId);

        List<T> entities = new ArrayList<>();
        for (Mutable mutable : mutables) {
            entities.add(e2m.convertMutableToEntity(mutable, entityClass));
        }
        return entities;
    }

    @Override
    public List<T> findSliceOfSeveralReferences(@NotNull List<FilterEntity> filterEntities, @NotNull Class<T> entityClass) {
        checkNull(entityClass);

        List<Mutable> mutables;
        mutables = m2db.getMutablesBySeveralReferences(
                ReflectionHelper.getValueFieldIds(entityClass),
                ReflectionHelper.getDateFieldIds(entityClass),
                ReflectionHelper.getListFieldIds(entityClass),
                ReflectionHelper.getReferenceFieldIds(entityClass), filterEntities);

        List<T> entities = new ArrayList<>();
        for (Mutable mutable : mutables) {
            entities.add(e2m.convertMutableToEntity(mutable, entityClass));
        }
        return entities;
    }

    @Override
    public T findEntityByReference(@NotNull BigInteger objectId, @NotNull Class<T> entityClass) {
        checkNull(entityClass);
        checkNull(objectId);

        Mutable mutable;
        mutable = m2db.getSingleMutableByReference(
                ReflectionHelper.getValueFieldIds(entityClass),
                ReflectionHelper.getDateFieldIds(entityClass),
                ReflectionHelper.getListFieldIds(entityClass),
                ReflectionHelper.getReferenceFieldIds(entityClass), objectId);

        return e2m.convertMutableToEntity(mutable, entityClass);
    }

    @Override
    public void delete(T entity) {
        checkNull(entity);

        deleteById(entity.getObjectId());
    }

    @Override
    public void deleteById(BigInteger objectId) {
        checkNull(objectId);

        if (m2db.countOfReferencesById(objectId).compareTo(BigInteger.valueOf(0)) == 1) {
            throw new InvalidDeleteException("Sorry, but you can't delete this item");
        }

        m2db.sqlDelete(objectId);
        log.info("Deleted object with Object_id {}", objectId);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        checkNull(entities);

        for (T entity : entities) {
            delete(entity);
        }
    }

    @Override
    public BigInteger count(Class<T> entityClass) {
        checkNull(entityClass);

        BigInteger objTypeId = ReflectionHelper.getObjTypeId(entityClass);
        return m2db.countById(objTypeId);
    }

    @Override
    public BigInteger count(Class<T> entityClass, List<FilterEntity> filterBy) {
        checkNull(entityClass);

        return m2db.countByFilter(ReflectionHelper.getValueFieldIds(entityClass),
                ReflectionHelper.getDateFieldIds(entityClass),
                ReflectionHelper.getListFieldIds(entityClass),
                ReflectionHelper.getReferenceFieldIds(entityClass), filterBy);
    }

    @Override
    public boolean existsById(BigInteger objectId) {
        checkNull(objectId);

        return m2db.existsByObjId(objectId);
    }

    /**
     * Checks if the argument is null. If so, it logs and throws an exception.
     *
     * @param o checked argument
     * @throws IllegalArgumentException if argument is null
     */
    private void checkNull(Object o) {
        if (o == null) {
            String message = "Provided object is null";
            RuntimeException exception = new IllegalArgumentException(message);
            log.error(message, exception);
            throw exception;
        }
    }

}

package com.nc.airport.backend.persistence.eav.repository;

import com.nc.airport.backend.model.BaseEntity;
import com.nc.airport.backend.persistence.eav.mutable2query.filtering2sorting.filtering.FilterEntity;
import com.nc.airport.backend.persistence.eav.mutable2query.filtering2sorting.sorting.SortEntity;
import org.springframework.data.repository.NoRepositoryBean;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * Interface for generic CRUD operations on a repository for a specific type.
 * The type should implement BaseEntity.
 */
@NoRepositoryBean
public interface EavCrudRepository<T extends BaseEntity> {

    /**
     * Saves a given entity.
     * Use the returned instance for further operations as the save operation might
     * have changed the entity instance completely.
     *
     * @param entity must not be null
     * @return the saved entity will never be null.
     */
    <S extends T> S save(@NotNull S entity);

    /**
     * Saves all given entities.
     *
     * @param entities must not be null.
     * @return the saved entities will never be null.
     */
    <S extends T> List<S> saveAll(@NotNull Iterable<S> entities);

    /**
     * Retrieves an entity by its object_id.
     *
     * @param objectId must not be {@literal null}.
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     * @throws IllegalArgumentException if {@code id} is {@literal null}.
     */
    Optional<T> findById(@NotNull BigInteger objectId, @NotNull Class<T> entityClass);

    /**
     * Returns all instances with the given object_ids.
     *
     * @param objectIds   ids instances of which are searched for. All the object_ids must be of a single type
     * @param entityClass specifies the type of instances
     * @return all instances of given type and given objectIds
     */
    List<T> findAll(@NotNull Class<T> entityClass, @NotNull Iterable<BigInteger> objectIds);

    /**
     * Returns all instances of given entityClass that are within row range.
     * Consider found instances form a list (this is where rows are from)
     *
     * @param entityClass search criteria
     * @param page        zero-based
     * @return slice of items that are found within specified rows
     */
    List<T> findSlice(@NotNull Class<T> entityClass, Page page);

    /**
     * Returns list of entities that are children of given entity
     *
     * @param entity parent of searched children
     * @param page   zero-based
     * @return list of entity's children
     */
    <CC extends BaseEntity> List<CC> findSliceOfChildren(@NotNull T entity, CC childClass, Page page);

    /**
     * Returns all instances of given class that are within row range, filtered and sortered.
     * Consider found instances form a list (this is where rows are from)
     *
     * @param entityClass search criteria
     * @param page        zero-based
     * @param sortBy      sorting criteria
     * @param filterBy    filtering criteria
     * @return slice of entities that are found and ordered with criterias
     */
    List<T> findSlice(@NotNull Class<T> entityClass,
                      Page page,
                      List<SortEntity> sortBy,
                      List<FilterEntity> filterBy);

    /**
     * Returns all children of given entity that are within row range, filtered and sortered.
     * Consider found instances form a list (this is where rows are from)
     *
     * @param entity   search criteria
     * @param page     zero-based
     * @param sortBy   sorting criteria
     * @param filterBy filtering criteria
     * @return slice of entities that are found and ordered with criterias
     */
    <CC extends BaseEntity> List<CC> findSliceOfChildren(@NotNull T entity,
                                CC childClass,
                                Page page,
                                List<SortEntity> sortBy,
                                List<FilterEntity> filterBy);

    /**
     * Deletes a given entity.
     *
     * @throws IllegalArgumentException in case the given entity is {@literal null}.
     */
    void delete(@NotNull T entity);

    /**
     * Deletes the entity with the given object_id.
     *
     * @param objectId must not be {@literal null}.
     * @throws IllegalArgumentException in case the given {@code id} is {@literal null}
     */
    void deleteById(@NotNull BigInteger objectId);

    /**
     * Deletes the given entities.
     *
     * @throws IllegalArgumentException in case the given {@link Iterable} is {@literal null}.
     */
    void deleteAll(@NotNull Iterable<? extends T> entities);

    /**
     * Returns the number of entities available. Entity is specified by its class.
     *
     * @param entityClass class that specifies what object is being searched. Must be not null
     * @return the number of entities
     */
    BigInteger count(@NotNull Class<T> entityClass);


    /**
     * Returns the number of entities available, filtered by criteria. Entity is specified by its class.
     *
     * @param entityClass class that specifies what object is being searched. Must be not null
     * @param filterBy    filtering criteria
     * @return the number of entities
     */
    BigInteger count(@NotNull Class<T> entityClass, List<FilterEntity> filterBy);

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param objectId must not be {@literal null}.
     * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
     * @throws IllegalArgumentException if {@code id} is {@literal null}.
     */
    boolean existsById(@NotNull BigInteger objectId);


}

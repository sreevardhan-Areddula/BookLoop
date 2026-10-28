package com.bookloop.book.repository;



import com.bookloop.book.entity.BookListing;
import com.bookloop.book.entity.BookOfferType;
import com.bookloop.book.entity.ListingAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookListingRepository extends JpaRepository<BookListing, UUID> {

    @Query("""
            select distinct listing from BookListing listing join listing.genres genre
            where (:title is null or lower(listing.title) like lower(concat('%', :title, '%')))
              and (:city is null or lower(listing.city) like lower(concat('%', :city, '%')))
              and (:genre is null or lower(genre) = lower(:genre))
              and (:offerType is null or listing.offerType = :offerType)
              and listing.availability = com.bookloop.book.entity.ListingAvailability.AVAILABLE
            order by listing.createdAt desc
            """)
    List<BookListing> search(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("city") String city,
            @Param("offerType") BookOfferType offerType
    );

    List<BookListing> findAllByOwner_IdOrderByCreatedAtDesc(UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select listing from BookListing listing where listing.id = :id")
    java.util.Optional<BookListing> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("update BookListing listing set listing.city = :city where listing.owner.id = :ownerId")
    int updateCityForOwner(@Param("ownerId") UUID ownerId, @Param("city") String city);
}

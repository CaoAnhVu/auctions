package com.example.auctions.repository;

import com.example.auctions.model.Auction;
import com.example.auctions.model.AuctionStatus;
import com.example.auctions.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Page<Auction> findBySeller(User seller, Pageable pageable);
    Page<Auction> findBySellerAndStatus(User seller, AuctionStatus status, Pageable pageable);
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime endTime);
    List<Auction> findBySellerId(Long sellerId);
} 
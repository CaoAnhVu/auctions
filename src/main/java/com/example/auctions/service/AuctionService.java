package com.example.auctions.service;

import com.example.auctions.model.Auction;
import com.example.auctions.model.AuctionStatus;
import com.example.auctions.model.User;
import com.example.auctions.repository.AuctionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuctionService {

    @Value("${auction.images.upload.path}")
    private String uploadPath;

    @Autowired
    private AuctionRepository auctionRepository;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadPath));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public List<Auction> getAuctionsBySeller(User seller) {
        return auctionRepository.findBySeller(seller, Pageable.unpaged()).getContent();
    }

    public List<Auction> getAuctionsBySellerAndStatus(User seller, AuctionStatus status) {
        return auctionRepository.findBySellerAndStatus(seller, status, Pageable.unpaged()).getContent();
    }

    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));
    }

    public Auction createAuction(Auction auction, String action) throws IOException {
        // Handle image upload
        if (auction.getImageFile() != null && !auction.getImageFile().isEmpty()) {
            String imageName = saveImage(auction.getImageFile());
            auction.setImage(imageName);
        }

        // Set status based on action
        if ("publish".equals(action)) {
            auction.setStatus(AuctionStatus.ACTIVE);
        } else {
            auction.setStatus(AuctionStatus.DRAFT);
        }

        return auctionRepository.save(auction);
    }

    public Auction updateAuction(Auction auction, String action) throws IOException {
        Auction existingAuction = getAuctionById(auction.getId());

        // Only allow updates if auction is in DRAFT status or hasn't started yet
        if (existingAuction.getStatus() != AuctionStatus.DRAFT && 
            existingAuction.getStatus() != AuctionStatus.ACTIVE && 
            LocalDateTime.now().isBefore(existingAuction.getEndTime())) {
            throw new RuntimeException("Cannot update auction that has already started or ended");
        }

        // Handle new image if provided
        if (auction.getImageFile() != null && !auction.getImageFile().isEmpty()) {
            // Delete old image if exists
            if (existingAuction.getImage() != null) {
                deleteImage(existingAuction.getImage());
            }
            // Save new image
            String imageName = saveImage(auction.getImageFile());
            auction.setImage(imageName);
        } else {
            auction.setImage(existingAuction.getImage());
        }

        // Handle status changes
        if ("publish".equals(action) && existingAuction.getStatus() == AuctionStatus.DRAFT) {
            auction.setStatus(AuctionStatus.ACTIVE);
        } else if ("delete".equals(action) && existingAuction.getStatus() == AuctionStatus.DRAFT) {
            deleteAuction(existingAuction);
            return null;
        } else {
            auction.setStatus(existingAuction.getStatus());
        }

        auction.setCreatedAt(existingAuction.getCreatedAt());
        auction.setSeller(existingAuction.getSeller());

        return auctionRepository.save(auction);
    }

    public void deleteAuction(Auction auction) {
        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new RuntimeException("Can only delete draft auctions");
        }

        // Delete image file if exists
        if (auction.getImage() != null) {
            deleteImage(auction.getImage());
        }

        // Delete auction from database
        auctionRepository.delete(auction);
    }

    private String saveImage(MultipartFile image) throws IOException {
        // Generate unique filename with original extension
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;
        
        // Create full path
        Path destinationPath = Paths.get(uploadPath).resolve(filename);
        
        // Ensure directory exists
        Files.createDirectories(destinationPath.getParent());
        
        // Copy file to destination
        Files.copy(image.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
        
        return filename;
    }

    private void deleteImage(String imageName) {
        try {
            Path imagePath = Paths.get(uploadPath).resolve(imageName);
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            // Log error but don't throw exception
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void updateExpiredAuctions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Auction> activeAuctions = auctionRepository.findByStatusAndEndTimeBefore(AuctionStatus.ACTIVE, now);
            
            System.out.println("Checking expired auctions at " + now); // Debug log
            System.out.println("Found " + activeAuctions.size() + " expired auctions"); // Debug log
            
            for (Auction auction : activeAuctions) {
                System.out.println("Updating auction " + auction.getId() + " to ENDED status"); // Debug log
                auction.setStatus(AuctionStatus.ENDED);
                auctionRepository.save(auction);
            }
        } catch (Exception e) {
            System.err.println("Error updating expired auctions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Page<Auction> getAuctionsBySeller(User seller, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return auctionRepository.findBySeller(seller, pageRequest);
    }

    public Page<Auction> getAuctionsBySellerAndStatus(User seller, AuctionStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return auctionRepository.findBySellerAndStatus(seller, status, pageRequest);
    }
}
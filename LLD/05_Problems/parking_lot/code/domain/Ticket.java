package Day4_problems.parking_lot.code.domain;

import java.time.LocalDateTime;

public class Ticket {
    

    private String ticketId;
    private String vehicleNumber;
    private int floorNumber;
    private int spotNumber;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;

    public Ticket(String ticketId, String vehicleNumber, int floorNumber, int spotNumber) {
        this.ticketId = ticketId;
        this.vehicleNumber = vehicleNumber;
        this.floorNumber = floorNumber;
        this.spotNumber = spotNumber;
        this.entryTime = LocalDateTime.now();
    }

    public int getSpotNumber() {
        return spotNumber;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public LocalDateTime getEntryDateTime () {
         return entryTime;
    }

    public LocalDateTime getExitDateTime() {
         return exitTime;
    }

}

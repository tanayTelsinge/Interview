package Day4_problems.parking_lot.code.strategies.spotallocationstrategy;

import java.util.List;

import Day4_problems.parking_lot.code.domain.ParkingSpot;
import Day4_problems.parking_lot.code.enums.SpotType;
import Day4_problems.parking_lot.code.enums.VehicleType;

public interface SpotAllocationStrategy {

    ParkingSpot allocateSpot(VehicleType vehicleType, List<ParkingSpot> spots);

    List<SpotType> getCompatibleSpotTypes(VehicleType vehicleType);
}

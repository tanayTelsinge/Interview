package Day4_problems.parking_lot.code.strategies.spotallocationstrategy;

import java.util.List;
import java.util.Map;

import Day4_problems.parking_lot.code.domain.ParkingSpot;
import Day4_problems.parking_lot.code.enums.SpotStatus;
import Day4_problems.parking_lot.code.enums.SpotType;
import Day4_problems.parking_lot.code.enums.VehicleType;


public class NearestSpotStrategy implements SpotAllocationStrategy {

    Map<VehicleType, List<SpotType>> vehicleSpotMap;

    public NearestSpotStrategy() {
        vehicleSpotMap = Map.of(
            VehicleType.CAR, List.of(SpotType.MEDIUM, SpotType.LARGE),
            VehicleType.TRUCK, List.of(SpotType.LARGE),
            VehicleType.BIKE, List.of(SpotType.COMPACT)
        );
    }
    @Override
    public ParkingSpot allocateSpot(VehicleType vehicleType, List<ParkingSpot> spots) {
        List<SpotType> suitableSpots = vehicleSpotMap.get(vehicleType);
        return spots.stream()
                .filter(spot -> suitableSpots.contains(spot.getSpotType()) && SpotStatus.AVAILABLE.equals(spot.getSpotStatus()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No available spot for vehicle type: " + vehicleType));
    }

    @Override
    public List<SpotType> getCompatibleSpotTypes(VehicleType vehicleType) {
        return vehicleSpotMap.get(vehicleType);
    }
}

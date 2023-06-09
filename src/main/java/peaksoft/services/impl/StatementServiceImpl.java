package peaksoft.services.impl;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import peaksoft.dto.requests.StatementRequest;
import peaksoft.dto.responses.SimpleResponse;
import peaksoft.dto.responses.StatementResponse;
import peaksoft.entity.Restaurant;
import peaksoft.entity.User;
import peaksoft.enums.Role;
import peaksoft.exeption.BadRequestException;
import peaksoft.exeption.NotFoundException;
import peaksoft.repositories.RestaurantRepository;
import peaksoft.repositories.UserRepository;
import peaksoft.services.StatementService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @author :ЛОКИ Kelsivbekov
 * @created 18.03.2023
 */
@Service
public class StatementServiceImpl implements StatementService {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;

    public StatementServiceImpl(UserRepository userRepository, RestaurantRepository restaurantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SimpleResponse saveStatement(StatementRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        long u = ChronoUnit.YEARS.between(request.dateOfBrith(), LocalDate.now());
        user.setDateOfBrith(u);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(request.role());
        user.setExperience(request.experience());

        userRepository.save(user);
        return SimpleResponse.builder()
                .status(HttpStatus.OK)
                .message(String.format(
                        "Statement with employee: %s successfully SAVED", request.firstName().concat(" " + request.lastName()))).build();
    }

    @Override
    public List<StatementResponse> findAll() {
        return userRepository.findAllNewEmployees();
    }

    @Override
    public SimpleResponse acceptOrDelete(Long restId, Long newStateId, Boolean acceptOrDel) {
        if (!restaurantRepository.existsById(restId)) {
            throw new NotFoundException(String.format("Restaurant with id: %d is not found", restId));
        }
        Restaurant restaurant = restaurantRepository.findById(restId).orElseThrow(() -> new NotFoundException(
                String.format("Restaurant with Id: %d doesn't exist", restId)));

        if (!userRepository.existsById(newStateId)) {
            throw new NotFoundException(String.format("Employee with id: %d is not found", newStateId));
        }

        User user = userRepository.findById(newStateId).orElseThrow(() -> new NotFoundException(
                String.format("Employee with id: %d is doesn't exist", newStateId)));

        if (acceptOrDel) {
            if (user.getRole().equals(Role.CHEF)) {
                restaurant.addChef(user);
                int count = restaurant.getUsers().size();
                if (count > 14) {
                    throw new BadRequestException("Sorry we haven't run out of vacancies");
                }
                user.setRestaurant(restaurant);
                userRepository.save(user);
                restaurant.setNumberOfEmployees(++count);
                restaurantRepository.save(restaurant);
                return SimpleResponse.builder().status(HttpStatus.OK)
                        .message(String.format("New Chef with full name: %s successfully SAVED",
                                user.getFirstName().concat(" " + user.getLastName()))).build();

            } else if (user.getRole().equals(Role.WAITER)) {

                restaurant.addWaiter(user);

                int count = restaurant.getUsers().size();
                if (count > 14) {
                    throw new BadRequestException("Sorry we haven't run out of vacancies");
                }

                user.setRestaurant(restaurant);
                restaurant.setNumberOfEmployees(++count);
                userRepository.save(user);
                restaurantRepository.save(restaurant);
                return SimpleResponse.builder().status(HttpStatus.OK)
                        .message(String.format("New Waiter with full name: %s successfully SAVED",
                                user.getFirstName().concat(" " + user.getLastName()))).build();
            }
        }

        userRepository.delete(user);
        throw new BadRequestException("Sorry we haven't run out of vacancies ");
    }
}

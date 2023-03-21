package peaksoft.services.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import peaksoft.dto.requests.ChequeRequest;
import peaksoft.dto.responses.*;
import peaksoft.entity.Cheque;
import peaksoft.entity.MenuItem;
import peaksoft.entity.Restaurant;
import peaksoft.entity.User;
import peaksoft.enums.Role;
import peaksoft.exeption.BadRequestException;
import peaksoft.exeption.NotFoundException;
import peaksoft.repositories.ChequeRepository;
import peaksoft.repositories.MenuItemRepository;
import peaksoft.repositories.RestaurantRepository;
import peaksoft.repositories.UserRepository;
import peaksoft.services.ChequeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author :ЛОКИ Kelsivbekov
 * @created 19.03.2023
 */
@Service
public class ChequeServiceImpl implements ChequeService {

    private final ChequeRepository chequeRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public ChequeServiceImpl(ChequeRepository chequeRepository, UserRepository userRepository, RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository) {
        this.chequeRepository = chequeRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;

    }

    @Override
    public SimpleResponse save(Long restaurantId, Long waiterId, ChequeRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(() -> new NotFoundException(String.format("Restaurant with id: %d doesn't exist",restaurantId)));
        User user = userRepository.findById(waiterId).orElseThrow(() -> new NotFoundException(
                String.format("Waiter with id: %d doesn't exist", waiterId)));

        if (!user.getRole().equals(Role.WAITER)) {
            throw new BadRequestException("Толко официянты могут зделать заказ!!");
        }

        Cheque cheque = new Cheque();
        List<MenuItem> menuItems = restaurant.getMenuItems();
        List<MenuItem> newMenuItems = new ArrayList<>();
        List<Long> id = request.getId();
        for (MenuItem menuItem : menuItems) {
            for (Long aLong : id) {
                if (menuItem.getId().equals(aLong)) {
                    newMenuItems.add(menuItem);
                }
            }
        }
        for (MenuItem newMenuItem : newMenuItems) {
            cheque.addMenuIterm(newMenuItem);
            newMenuItem.addCheque(cheque);
        }


        cheque.setUser(user);
        cheque.setCreatedAd(LocalDate.now());
        user.addCheque(cheque);

        chequeRepository.save(cheque);

        return SimpleResponse.builder()
                .status(HttpStatus.OK)
                .message(String.format("Cheque with id: %d is successfully SAVED", cheque.getId()))
                .build();
    }

    @Override
    public List<ChequeResponse> findAll(Long waiterId) {
        User waiter = userRepository.findById(waiterId).orElseThrow(() -> new NotFoundException(String.format("Waiter with id: %d doesn't exist", waiterId)));
        List<Cheque> cheques = waiter.getCheques();
        return convertList1(cheques);
    }

    @Override
    public SimpleResponse update(Long waiterId, Long chequeId, ChequeRequest chequeRequest) {

        User waiter = userRepository.findById(waiterId).orElseThrow(() -> new NotFoundException(String.format("Waiter with id: %d doesn't exist", waiterId)));

        List<Long> menuItemIds = chequeRequest.getId();
        Cheque cheque = chequeRepository.findById(chequeId).orElseThrow();
        List<MenuItem> menuItems1 = cheque.getMenuItems();
        List<MenuItem> menuItems = menuItemRepository.findAll();

        for (MenuItem menuItem : menuItems) {
            for (Long menuItemId : menuItemIds) {
                if (menuItemId.equals(menuItem.getId())) {
                    cheque.addMenuIterm(menuItem);
                    menuItems1.add(menuItem);
                    menuItem.addCheque(cheque);
                    chequeRepository.save(cheque);
                }
            }
        }


        return SimpleResponse.builder().
                status(HttpStatus.OK)
                .message(String.format("Cheque with id: %d is successfully UPDATED", chequeId))
                .build();
    }

    @Override
    public SimpleResponse delete(Long waiterId, Long chequeId) {
        if (!userRepository.existsById(waiterId)){
            throw new NotFoundException(String.format("Waiter with Id: %d doesnt exist", waiterId));
        }
        if (!userRepository.existsById(chequeId)){
            throw new NotFoundException(String.format("Cheque with id: %d doesn't exist", chequeId));
        }
        User waiter = userRepository.findById(waiterId).orElseThrow(() -> new NotFoundException(String.format("Waiter with id: %d doesn't exist", waiterId)));

        Cheque cheque = chequeRepository.findById(chequeId).orElseThrow();
        cheque.getMenuItems().removeAll(cheque.getMenuItems());
        waiter.getCheques().removeIf(c -> c.getId().equals(chequeId));
        chequeRepository.delete(cheque);

        return SimpleResponse.builder()
                .status(HttpStatus.OK)
                .message(String.format("Cheque with id: %d successfully DELETED",chequeId)).build();
    }


    private ManuResponse convert(MenuItem menuItem) {
        return ManuResponse.builder()
                .id(menuItem.getId())
                .description(menuItem.getDescription())
                .image(menuItem.getImage())
                .price(menuItem.getPrice())
                .name(menuItem.getName())
                .isVegetarian(menuItem.getIsVegetarian()).build();
    }

    private List<ManuResponse> convertList(List<MenuItem> menuItems) {
        List<ManuResponse> manuResponses = new ArrayList<>();
        for (MenuItem menuItem : menuItems) {
            manuResponses.add(convert(menuItem));
        }
        return manuResponses;
    }

    private ChequeResponse convert(Cheque cheque) {
        return ChequeResponse.builder()
                .id(cheque.getId())
                .firstName(cheque.getUser().getFirstName())
                .lastName(cheque.getUser().getLastName())
                .service(cheque.getUser().getRestaurant().getService())
                .grandTotal(averagePriceWithService(cheque.getMenuItems().stream().map(MenuItem::getPrice).toList(),cheque.getUser().getRestaurant().getService()))
                .averagePrice(averagePrice(cheque.getMenuItems().stream().map(MenuItem::getPrice).toList()))
                .manuResponses(convertList(cheque.getMenuItems())).build();
    }

    private List<ChequeResponse> convertList1(List<Cheque> cheques) {
        List<ChequeResponse> chequeResponses = new ArrayList<>();
        for (Cheque cheque : cheques) {
            chequeResponses.add(convert(cheque));
        }
        return chequeResponses;
    }

    private BigDecimal averagePrice(List<BigDecimal> prices) {
        return prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal averagePriceWithService(List<BigDecimal> prices, Integer service) {
        BigDecimal reduce = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        int i = reduce.intValue();
        i = i+(i * service / i);
        return BigDecimal.valueOf(i);
    }

//    @Override
//    public SimpleResponse1 oneDayAveragePrice(Long waiterId){
//        User waiter = userRepository.findById(waiterId).orElseThrow(() -> new NotFoundException(String.format("Waiter with id: %d doesn't exist", waiterId)));
//
//        List<BigDecimal> price= new ArrayList<>();
//        List<Cheque> cheques = waiter.getCheques();
//        for (Cheque cheque : cheques) {
//            List<MenuItem> menuItems = cheque.getMenuItems();
//            if (cheque.getCreatedAd().equals(LocalDate.now())) {
//                for (MenuItem menuItem : menuItems) {
//                    price.add(menuItem.getPrice());
//                }
//            }
//        }
//        return SimpleResponse1.builder().totalPrice(price.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).build();
//    }

}

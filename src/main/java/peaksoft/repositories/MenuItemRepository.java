package peaksoft.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import peaksoft.dto.responses.ManuResponse;
import peaksoft.entity.MenuItem;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    @Query("select new peaksoft.dto.responses.ManuResponse(m.id, m.name, m.image, m.price, m.description, m.isVegetarian)" +
           " from MenuItem m where m.restaurant.id=?1 and m.isVegetarian = ?2 order by m.price asc")
    List<ManuResponse> findAllMenusAsc(Long restId, Boolean i);

    @Query("select new peaksoft.dto.responses.ManuResponse(m.id, m.name, m.image, m.price, m.description, m.isVegetarian)" +
           " from MenuItem m where m.restaurant.id=?1 and m.isVegetarian=?2 order by m.price desc ")
    List<ManuResponse> findAllMenusDesc(Long restId, Boolean i);
    @Query("select new peaksoft.dto.responses.ManuResponse(m.id, m.name, m.image, m.price, m.description, m.isVegetarian)" +
           " from MenuItem m where m.id=?1")
    Optional<ManuResponse> findByMenuId(Long menuId);
}
package com.salgueros.asistencia.repository;

import com.salgueros.asistencia.model.EstadoRemision;
import com.salgueros.asistencia.model.Remision;
import com.salgueros.asistencia.model.TipoRemision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RemisionRepository extends JpaRepository<Remision, Long> {

    // Consecutivo máximo filtrado por tipo (INSUMOS o COMIDA)
    @Query("SELECT MAX(r.numeroRemision) FROM Remision r WHERE r.tipo = :tipo")
    Optional<Integer> findMaxNumeroRemisionPorTipo(@Param("tipo") TipoRemision tipo);

    Optional<Remision> findByTipoAndNumeroRemision(TipoRemision tipo, Integer numeroRemision);

    List<Remision> findByTipoAndEstadoOrderByNumeroRemisionDesc(TipoRemision tipo, EstadoRemision estado);

    List<Remision> findByTipoAndPuntoVentaOrderByNumeroRemisionDesc(TipoRemision tipo, String puntoVenta);

    List<Remision> findByPuntoVentaOrderByNumeroRemisionDesc(String puntoVenta);

    List<Remision> findByTipoOrderByNumeroRemisionDesc(TipoRemision tipo);

    List<Remision> findByFechaBetweenOrderByNumeroRemisionDesc(LocalDate fechaInicio, LocalDate fechaFin);
}
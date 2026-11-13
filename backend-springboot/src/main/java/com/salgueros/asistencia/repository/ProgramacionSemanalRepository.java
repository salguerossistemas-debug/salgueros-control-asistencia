package com.salgueros.asistencia.repository;

import com.salgueros.asistencia.model.Empleado;
import com.salgueros.asistencia.model.ProgramacionSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramacionSemanalRepository extends JpaRepository<ProgramacionSemanal, Long> {
    Optional<ProgramacionSemanal> findByEmpleadoAndFecha(Empleado empleado, LocalDate fecha);
    List<ProgramacionSemanal> findByFechaBetween(LocalDate inicio, LocalDate fin);

    @Query("SELECT DISTINCT p FROM ProgramacionSemanal p " +
           "JOIN FETCH p.empleado e " +
           "WHERE p.fecha BETWEEN :inicio AND :fin " +
           "ORDER BY p.fecha ASC, e.area ASC, e.nombreApellido ASC")
    List<ProgramacionSemanal> findProgramacionOrdenada(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Modifying
    @Transactional
    @Query("UPDATE ProgramacionSemanal p SET p.bloqueado = :bloqueado WHERE p.fecha BETWEEN :inicio AND :fin")
    int actualizarBloqueoEnRango(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin, @Param("bloqueado") boolean bloqueado);
}

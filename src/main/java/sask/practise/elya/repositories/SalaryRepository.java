package sask.practise.elya.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sask.practise.elya.models.Salary;

import java.util.List;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long>, JpaSpecificationExecutor<Salary> {
    @Query("SELECT s FROM Salary s LEFT JOIN FETCH s.employee WHERE s.employee.id = :employeeId")
    List<Salary> findByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId")
    long countEmployeesByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT s FROM Salary s LEFT JOIN FETCH s.employee")
    List<Salary> findAllWithEmployees();

    // Средняя зарплата
    @Query("SELECT AVG(s.amount) FROM Salary s")
    Double findAverageSalary();

    // Количество сотрудников с зарплатой выше средней
    @Query("SELECT COUNT(s) FROM Salary s WHERE s.amount > (SELECT AVG(s2.amount) FROM Salary s2)")
    Long countEmployeesWithSalaryAboveAverage();

    // Максимальная зарплата
    @Query("SELECT MAX(s.amount) FROM Salary s")
    Double findMaxSalary();

    // Минимальная зарплата
    @Query("SELECT MIN(s.amount) FROM Salary s")
    Double findMinSalary();

    // Средняя зарплата по отделам
    @Query("SELECT e.department.name, AVG(s.amount) FROM Salary s JOIN s.employee e GROUP BY e.department.name")
    List<Object[]> findAverageSalaryByDepartment();

    // Сотрудники с зарплатой выше средней
    @Query("SELECT e.fullName, s.amount FROM Salary s JOIN s.employee e WHERE s.amount > (SELECT AVG(s2.amount) FROM Salary s2) ORDER BY s.amount DESC")
    List<Object[]> findEmployeesWithSalaryAboveAverage();
}

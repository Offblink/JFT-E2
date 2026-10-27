package com.offblink.web;

import com.offblink.entity.Emp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JSR-303 校验规则的单测（任务 6.1）。
 *
 * 不经过 Controller：直接拿 Validator 校验实体，
 * 这样规则的失败与 Web 层无关，接口层的表现另外用 curl/ApiFox 验。
 */
class EmpValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void blankNameAndNegativeSalaryAreRejected() {
        Emp emp = new Emp();
        emp.setEmpName("   ");                        // 全空白 → NotBlank 不通过
        emp.setSalary(new BigDecimal("-1"));          // 负数 → DecimalMin 不通过

        Set<String> violated = validator.validate(emp).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(violated.contains("empName"), "姓名应被判定为空：实际违规字段 " + violated);
        assertTrue(violated.contains("salary"), "负薪资应被拒绝：实际违规字段 " + violated);
    }

    @Test
    void nullSalaryIsRejected() {
        Emp emp = new Emp();
        emp.setEmpName("张三");
        emp.setSalary(null);

        Set<String> violated = validator.validate(emp).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertTrue(violated.contains("salary"));
    }

    @Test
    void oversizedNameIsRejected() {
        Emp emp = new Emp();
        emp.setEmpName(repeat("长", 51));
        emp.setSalary(new BigDecimal("8000.00"));

        Set<String> violated = validator.validate(emp).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(violated.contains("empName"));
    }

    @Test
    void validEmployeePasses() {
        Emp emp = new Emp();
        emp.setEmpName("张三");
        emp.setSalary(new BigDecimal("8000.00"));

        assertTrue(validator.validate(emp).isEmpty());
    }

    private static String repeat(String unit, int times) {
        StringBuilder sb = new StringBuilder(unit.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(unit);
        }
        return sb.toString();
    }
}

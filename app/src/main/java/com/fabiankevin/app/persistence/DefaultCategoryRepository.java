package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.CategorySummary;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.entities.projections.CategorySummaryProjection;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.services.queries.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class DefaultCategoryRepository implements CategoryRepository {
    private final JpaCategoryRepository jpaCategoryRepository;

    @Override
    public Optional<Category> findByIdAndUserId(UUID id, UUID userId) {
        return jpaCategoryRepository.findByIdAndUserId(id, userId)
                .map(CategoryEntity::toModel);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaCategoryRepository.findById(id)
                .map(CategoryEntity::toModel);
    }

    @Override
    public boolean existsByNameAndTypeAndUserId(String name, TransactionType type, UUID userId) {
        return jpaCategoryRepository.existsByNameAndTransactionTypeAndUserId(name, type, userId);
    }

    @Override
    public Optional<Category> findInactiveByNameAndTypeAndUserId(String name, TransactionType type, UUID userId) {
        return jpaCategoryRepository.findFirstByActiveFalseAndNameAndTransactionTypeAndUserId(name, type, userId)
                .map(CategoryEntity::toModel);
    }

    @Override
    public Optional<Category> findByNameAndTypeAndUserId(String name, TransactionType type, UUID userId) {
        return jpaCategoryRepository.findFirstByNameAndTransactionTypeAndUserId(name, type, userId)
                .map(CategoryEntity::toModel);
    }

    @Override
    public Category save(Category category) {
        CategoryEntity saved = jpaCategoryRepository.save(CategoryEntity.from(category));
        return saved.toModel();
    }

    @Override
    public int deleteByIdAndUserId(UUID id, UUID userId) {
        return jpaCategoryRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public com.fabiankevin.app.models.Page<Category> findAllByPageQuery(PageQuery query, UUID userId, TransactionType type) {
        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort())
        );
        var entityPage  = Optional.ofNullable(type)
                .map(t -> jpaCategoryRepository.findAllByUserIdAndTransactionType(userId, t, pageable))
                .orElseGet(() -> jpaCategoryRepository.findAllByUserId(userId, pageable))
                .map(CategoryEntity::toModel);

        return new com.fabiankevin.app.models.Page<>(
                entityPage.getContent(),
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast(),
                entityPage.isFirst()
        );
    }

    @Override
    public com.fabiankevin.app.models.Page<CategorySummary> findAllByPageQueryWithSummary(PageQuery query, UUID userId, TransactionType type) {
        var now = LocalDate.now();
        var monthStart = now.withDayOfMonth(1);
        var monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort())
        );
        var entityPage = jpaCategoryRepository.findAllByUserIdAndTransactionTypeWithSummary(userId, type, monthStart, monthEnd, pageable);

        // Calculate percentages for each category
        double expenseTotalAmount = entityPage.getContent().stream()
                .filter(category -> category.type() == TransactionType.EXPENSE)
                .mapToDouble(CategorySummaryProjection::amount)
                .sum();
        double incomeTotalAmount = entityPage.getContent().stream()
                .filter(category -> category.type() == TransactionType.INCOME)
                .mapToDouble(CategorySummaryProjection::amount)
                .sum();

        List<CategorySummary> content = entityPage.getContent().stream()
                .map(projection -> {
                    double percentage = getPercentage(projection, projection.type() == TransactionType.EXPENSE ? expenseTotalAmount : incomeTotalAmount);
                    return CategorySummary.builder()
                            .id(projection.id())
                            .name(projection.name())
                            .type(projection.type())
                            .userId(projection.userId())
                            .icon(projection.icon())
                            .active(projection.active())
                            .system(false)
                            .totalAmount(projection.amount())
                            .percentage(percentage)
                            .totalTransactions(projection.totalTransactions())
                            .build();
                })
                .collect(Collectors.toList());

        return new com.fabiankevin.app.models.Page<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast(),
                entityPage.isFirst()
        );
    }

    private static double getPercentage(CategorySummaryProjection projection, double expenseTotalAmount) {
        return (expenseTotalAmount != 0 && projection.amount() != 0)
                ? (projection.amount() / expenseTotalAmount) * 100.0
                : 0.0;
    }

    @Override
    public List<Category> findAllByNamesIn(List<String> names) {
        return jpaCategoryRepository.findAllByNameIn(names)
                .stream()
                .map(CategoryEntity::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public long deleteAllByUserId(UUID userId) {
        return jpaCategoryRepository.deleteAllByUserId(userId);
    }
}

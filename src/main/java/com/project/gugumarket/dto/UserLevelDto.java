package com.project.gugumarket.dto;

import com.project.gugumarket.entity.User;
import com.project.gugumarket.entity.UserLevel;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLevelDto {

    private String level;
    private String levelName;
    private String emoji;
    private Integer transactionCount;
    private Integer toNextLevel;
    private Integer minTransactions;
    private Integer maxTransactions;

    public static UserLevelDto from(User user) {
        if (user == null) {
            return null;
        }

        UserLevel level = user.getUserLevel() != null ? user.getUserLevel() : UserLevel.EGG;
        Integer count = user.getTransactionCount() != null ? user.getTransactionCount() : 0;

        return UserLevelDto.builder()
                .level(level.name())
                .levelName(level.getDisplayName())
                .emoji(level.getEmoji())
                .transactionCount(count)
                .toNextLevel(level.getTransactionsToNextLevel(count))
                .minTransactions(level.getMinTransactions())
                .maxTransactions(level == UserLevel.ADULT_BIRD ? null : level.getMaxTransactions())
                .build();
    }
}
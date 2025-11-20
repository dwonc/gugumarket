package com.project.gugumarket.dto;

import com.project.gugumarket.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 *  간단한 사용자 정보 DTO
 *  사용처:
 *   - 구매 희망자 목록
 *   - 판매자 정보
 *   - 댓글 작성자 정보
 */

@Getter
@NoArgsConstructor
public class UserSimpleResponse {

  private Long userId;
  private String username;
  private String nickname;
  private String profileImage; //프로필 이미지가 있다면

  /**
     * 🔄 Entity → DTO 변환
     */
      public static UserSimpleResponse from(User user) {
        UserSimpleResponse dto = new UserSimpleResponse();
        dto.userId = user.getUserId();
        dto.username = user.getUserName();
        dto.nickname = user.getNickname() != null ? user.getNickname() : user.getUserName();
        // dto.profileImage = user.getProfileImage();  // 필요하면 추가
        return dto;
  }
  
}

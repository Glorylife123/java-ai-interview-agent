package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM `user` WHERE id = #{id} AND deleted = 0")
    User selectById(Long id);

    @Select("SELECT * FROM `user` WHERE username = #{username} AND deleted = 0")
    User selectByUsername(String username);

    @Select("SELECT * FROM `user` WHERE deleted = 0 ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}")
    List<User> selectPage(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(*) FROM `user` WHERE deleted = 0")
    long countAll();

    @Insert("""
            INSERT INTO `user` (username, password_hash, nickname, avatar_url, role, status)
            VALUES (#{username}, #{passwordHash}, #{nickname}, #{avatarUrl}, #{role}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("""
            <script>
            UPDATE `user`
            <set>
                <if test="nickname != null">nickname = #{nickname},</if>
                <if test="avatarUrl != null">avatar_url = #{avatarUrl},</if>
                <if test="role != null">role = #{role},</if>
                <if test="status != null">status = #{status},</if>
            </set>
            WHERE id = #{id} AND deleted = 0
            </script>
            """)
    int updateById(User user);

    @Update("UPDATE `user` SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int logicDeleteById(Long id);
}

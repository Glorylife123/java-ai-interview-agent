package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.Tag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TagMapper {

    @Select("SELECT * FROM tag WHERE id = #{id} AND deleted = 0")
    Tag selectById(Long id);

    @Select("SELECT * FROM tag WHERE deleted = 0 ORDER BY category, name")
    List<Tag> selectAll();

    @Insert("INSERT INTO tag (name, category) VALUES (#{name}, #{category})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    @Update("""
            <script>
            UPDATE tag
            <set>
                <if test="name != null">name = #{name},</if>
                <if test="category != null">category = #{category},</if>
            </set>
            WHERE id = #{id} AND deleted = 0
            </script>
            """)
    int updateById(Tag tag);

    @Update("UPDATE tag SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int logicDeleteById(Long id);
}

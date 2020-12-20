package ${packageName};

import org.arch.framework.crud.dto.BaseRequestDto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
<#list imports as import>
import ${import};
</#list>

/**
 *  ${entity.className} Dto generated by auto
 * ${comments}
 *
 * @author ${author}
 * @since ${date}.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class ${className} extends BaseRequestDto {

    /**
     * 
     */
    private static final long serialVersionUID = -1L;

    <#if entity.fields?? && (entity.fields?size > 0)>
    <#list entity.fields as f>
    /**
     * TODO 字段信息描述
     */
    private ${f.className} ${f.name};

    </#list>
    </#if>
}
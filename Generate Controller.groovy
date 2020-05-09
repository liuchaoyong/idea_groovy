import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

import java.text.SimpleDateFormat
import java.util.Map
import java.util.HashMap

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "cn.chendd.blog;"//默认的包路径，后续替换
author = "liucy";
pkIdText = "id";//约定含有“ID”的文本注释列为主键
dateTime = getDateTime();
typeMapping = [
        (~/(?i)bigint/)                   : "Long",
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)tinyint/)                  : "Boolean",
        (~/(?i)datetime|timestamp|date/)  : "Date",
        (~/(?i)blob|binary|bfile|clob|raw|image/): "InputStream",
        (~/(?i)/)                         : "String"
]

def getPackageName(dir) {
  return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".")
          .replaceAll("^.*src(\\.main\\.java\\.)?", "") + ";"
}

def getDateTime(){
  return new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date());
}

def getImportMapping(type){
  Map<String , String> map = new HashMap();
  map.put("Date" , "java.util.Date")
  map.put("InputStream" , "java.io.InputStream")
  if(map.containsKey(type)){
    return map.get(type)
  }
  return type
}

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + "Controller.java").withPrintWriter("UTF-8") { out -> generate(dir , table , out, className, fields) }
}

/**
 * 生成的时候，选择项目的代码路径，从包路径中找src/java/main路径后的路径开始替换
 */
def generate(dir, table, out, className, fields) {
  packageName = getPackageName(dir);
  tableName = table.getName();
  classNameLower = className[0..0].toLowerCase() +className[1..-1]
  out.println "package $packageName"
  out.println ""
  //约定按照列的描述中含有主键ID的字段备注为主键，某些关系表中的第一个字段不一定就是主键
  existPk(fields , out);
  /////////////////////////////////////
  out.println "import com.pass.blood.smm.common.enums.RespEnum;"
  out.println "import com.pass.blood.smm.common.model.Resp;"
  out.println "import com.pass.blood.smm.common.utils.BeanUtil;"
  out.println "import com.pass.blood.smm.common.utils.RespUtil;"
  out.println "import "+packageName[0..-13]+".domain.${className};"
  out.println "import "+packageName[0..-13]+".dto.${className}SaveDTO;"
  out.println "import "+packageName[0..-13]+".dto.${className}UpdateDTO;"
  out.println "import "+packageName[0..-13]+".service.${className}Service;"
  out.println "import "+packageName[0..-13]+".vo.${className}VO;"
  //out.println "import io.swagger.annotations.Api;"
  out.println "import io.swagger.annotations.ApiOperation;"
  out.println "import io.swagger.annotations.ApiParam;"
  out.println "import org.springframework.beans.BeanUtils;"
  out.println "import org.springframework.web.bind.annotation.*;"

  out.println ""
  out.println "/**"
  out.println " * " + table.getComment() + "controller"
  out.println " * @auth $author"
  out.println " * @date $dateTime"
  out.println " */"
  out.println "@RestController"
  out.println "@RequestMapping(\"/${className}\")"
  out.println "public class "+className+"Controller {"
  out.println ""
  out.println "    private final "+className+"Service "+classNameLower+"Service;"
  out.println ""
  out.println "    public "+className+"Controller("+className+"Service "+classNameLower+"Service) {"
  out.println "        this."+classNameLower+"Service = "+classNameLower+"Service;"
  out.println "    }"
  out.println ""
  out.println "  @GetMapping(\"/query/{id}\")"
  out.println "  @ApiOperation(\"Find By Id\")"
  out.println "  public Resp<${className}VO> show(@ApiParam(\"ID\") @PathVariable(\"id\") Long id) {"
  out.println "    ${className} ${classNameLower} = ${classNameLower}Service.getById(id);"
  out.println "    //can not find"
  out.println "    if (null == ${classNameLower}) {"
  out.println "      return RespUtil.error(RespEnum.DATA_NOT_FOUND);"
  out.println "    }"
  out.println "    ${className}VO ${classNameLower}VO = BeanUtil.copyProperties(${classNameLower}, ${className}VO::new);"
  out.println "    return RespUtil.success(${classNameLower}VO);"
  out.println "  }"
  out.println ""
  out.println "    @DeleteMapping(\"/delete/{id}\")"
  out.println "    @ApiOperation(\"Delete by id\")"
  out.println "    public Resp delete(@ApiParam(\"ID\") @PathVariable(\"id\") Long id) {"
  out.println "        ${className} ${classNameLower} = ${classNameLower}Service.getById(id);"
  out.println "        //can not find"
  out.println "        if (null == ${classNameLower}) {"
  out.println "            return RespUtil.error(RespEnum.DATA_NOT_FOUND);"
  out.println "        }"
  out.println "        Boolean isDelete = ${classNameLower}Service.removeById(id);"
  out.println "        if (isDelete) {"
  out.println "            return RespUtil.success(RespEnum.SUCCESS);"
  out.println "        } else {"
  out.println "            return RespUtil.error(RespEnum.DELETE_FAIL);"
  out.println "        }"
  out.println "    }"
  out.println ""
  out.println "    @PostMapping(\"/add\")"
  out.println "    @ApiOperation(\"Insert\")"
  out.println "    public Resp<${className}VO> add(${className}SaveDTO ${classNameLower}SaveDTO) {"
  out.println "        ${className} ${classNameLower} = BeanUtil.copyProperties(${classNameLower}SaveDTO, ${className}::new);"
  out.println "        Boolean isSave = ${classNameLower}Service.save(${classNameLower});"
  out.println "        if (isSave) {"
  out.println "            ${className}VO ${classNameLower}VO = BeanUtil.copyProperties(${classNameLower}, ${className}VO::new);"
  out.println "            return RespUtil.success(${classNameLower}VO);"
  out.println "        } else {"
  out.println "            return RespUtil.error(RespEnum.INSERT_FAIL);"
  out.println "        }"
  out.println "    }"
  out.println ""
  out.println "    @PostMapping(\"/update\")"
  out.println "    @ApiOperation(\"update\")"
  out.println "    public Resp<${className}VO> update(@RequestBody ${className}UpdateDTO ${classNameLower}UpdateDTO) {"
  out.println "        ${className} ${classNameLower} = ${classNameLower}Service.getById(${classNameLower}UpdateDTO.getId());"
  out.println "        //can not find"
  out.println "        if (null == ${classNameLower}) {"
  out.println "            return RespUtil.error(RespEnum.DATA_NOT_FOUND);"
  out.println "        }"
  out.println "        BeanUtils.copyProperties(${classNameLower}UpdateDTO, ${classNameLower});"
  out.println "        Boolean isUpdate = ${classNameLower}Service.updateById(${classNameLower});"
  out.println "        if (isUpdate) {"
  out.println "            ${className}VO ${classNameLower}VO = BeanUtil.copyProperties(${classNameLower}, ${className}VO::new);"
  out.println "            return RespUtil.success(${classNameLower}VO);"
  out.println "        } else {"
  out.println "            return RespUtil.error(RespEnum.UPDATE_FAIL);"
  out.println "        }"
  out.println "    }"
  out.println "}"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[       fieldName : col.getName(),
                       name : javaName(col.getName(), false), //表字段名称转为Java命名
                       type : typeStr, //表字段类型
                       comment: col.getComment(), //表字段注释
                       annos: ""]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
          .collect { Case.LOWER.apply(it).capitalize() }
          .join("")
          .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def existPk(fields , out){
  fields.each() {
//    if (fields.comment.indexOf(pkIdText) != -1){
//      out.println "import org.hibernate."
//    }

  }
}
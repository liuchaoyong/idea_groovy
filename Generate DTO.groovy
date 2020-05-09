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
pkIdText = "\u4e3b\u952eID";//约定含有“主键ID”的文本注释列为主键
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
  className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + "DTO.java").withPrintWriter("UTF-8") { out -> generate(dir , table , out, className+"DTO", fields) }
  new File(dir, className + "SaveDTO.java").withPrintWriter("UTF-8") { out -> generate(dir , table , out, className+"SaveDTO", fields) }
  new File(dir, className + "UpdateDTO.java").withPrintWriter("UTF-8") { out -> generate(dir , table , out, className+"UpdateDTO", fields) }

}

/**
 * 生成的时候，选择项目的代码路径，从包路径中找src/java/main路径后的路径开始替换
 */
def generate(dir, table, out, className, fields) {
  packageName = getPackageName(dir);
  tableName = table.getName();
  out.println "package $packageName"
  out.println ""
  //约定按照列的描述中含有主键ID的字段备注为主键，某些关系表中的第一个字段不一定就是主键
  existPk(fields , out);
  out.println "import io.swagger.annotations.ApiModel;"
  out.println "import io.swagger.annotations.ApiModelProperty;"
  out.println "import lombok.Data;"
  out.println ""
  out.println "/**"
  out.println " * " + table.getComment()
  out.println " * @auth $author"
  out.println " * @date $dateTime"
  out.println " */"
  out.println "@Data"
  out.println "@ApiModel(value=\""+ className +"\" ,description =\"" + table.getComment() + "\")"
  out.println "public class " +  className  + "{"
  out.println ""
  fields.each() {
    if (it.annos != "") out.println "  ${it.annos}"

    if (it.name == "dataStatus"){
      out.println "    @JsonIgnore"
      out.println "    @ApiModelProperty(value = \"\u6570\u636e\u72b6\u6001\uff0c\u53ef\u7528\u6216\u7981\u7528\" , example = \"DISABLE\" , hidden = true)"
    } else {
      out.println "    @ApiModelProperty(value = \"${it.comment}\")"
    }
    out.println "    private " + getImportMapping(it.type) + " ${it.name};"
    out.println ""
  }
  out.println ""
  out.println "}"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
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


  }
}
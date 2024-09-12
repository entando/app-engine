/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.plugins.jpwebform.aps.system.init.servdb;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Date;

@DatabaseTable(tableName = Form.TABLE_NAME)
public class Form {
	
	public Form() {}
	
	@DatabaseField(columnName = "id", 
		dataType = DataType.INTEGER, 
		 canBeNull=false, id = true)
	private int _id;
	
	@DatabaseField(columnName = "name", 
		dataType = DataType.LONG_STRING,
		 canBeNull=false)
	private String _name;
	
	@DatabaseField(columnName = "submitted", 
			dataType = DataType.DATE,
		 canBeNull= true)
	private Date _submitted;
	
	@DatabaseField(columnName = "data", 
		dataType = DataType.LONG_STRING,
		 canBeNull=false)
	private String _data;
	

public static final String TABLE_NAME = "jpwebform_form";
}

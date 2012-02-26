/*
 * DerbyTableSourceBuilder
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.derby;

import workbench.db.ColumnIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DerbyTableSourceBuilder
	extends TableSourceBuilder
{

	public DerbyTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	protected String getColumnSQL(ColumnIdentifier column, int maxTypeLength, String columnConstraint)
	{
		String defaultValue = column.getDefaultValue();
		if (StringUtil.isNonBlank(defaultValue) && defaultValue.startsWith("AUTOINCREMENT:"))
		{
			StringBuilder sql = new StringBuilder(100);
			sql.append(StringUtil.padRight(column.getDbmsType(), maxTypeLength));
			sql.append(" GENERATED ALWAYS AS IDENTITY");
			// "start 5 increment 10";
			String options = defaultValue.substring("AUTOINCREMENT:".length() + 1).toLowerCase();
			options = options.replace("start", "START WITH");
			options = options.replace(" increment", ", INCREMENT BY");
			sql.append(" (");
			sql.append(options);
			sql.append(")");
			return sql.toString();
		}
		else if (StringUtil.isNonBlank(defaultValue) && defaultValue.equals("GENERATED_BY_DEFAULT"))
		{
			StringBuilder sql = new StringBuilder(100);
			sql.append(StringUtil.padRight(column.getDbmsType(), maxTypeLength));
			sql.append(" GENERATED BY DEFAULT AS IDENTITY");
			return sql.toString();
		}
		else
		{
			return super.getColumnSQL(column, maxTypeLength, columnConstraint);
		}
	}

}

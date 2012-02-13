/*****************************************************************************

@header@
@date@
@copyright@
@license@
Created by charles.hache@gmail.com on Feb 11, 2012

*****************************************************************************/
package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;

/**
 * Gets the value for the current operand.
 */
public class Operand extends PostfixMathCommand
{
	
	public Operand() {
		numberOfParameters = 1;
	}

	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(operand(param));//push the result on the inStack
		return;
	}
	

	public Object operand(Object param) throws ParseException 
	{
		if (param instanceof Number) 
		{
			return new Double(((Number) param).doubleValue());
		}
		throw new ParseException("Invalid parameter type");
	}
	

}

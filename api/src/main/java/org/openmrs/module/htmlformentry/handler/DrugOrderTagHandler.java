package org.openmrs.module.htmlformentry.handler;

import java.util.List;

import org.openmrs.OrderType;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.widget.OrderWidgetConfig;

/**
 * Handles the {@code <drugOrder>} tag. This exists in order to provide a convenience over the
 * existing "order" tag, by simply implying an OrderType based on
 * HtmlformentryUtil.getDrugOrderTypes();
 */
public class DrugOrderTagHandler extends OrderTagHandler {
	
	/**
	 * support explicit configuration of orderType on the tag, as is supported by the order tag
	 * superclass if not supplied, infer the order type if only one valid order type is configured for
	 * drug orders
	 */
	@Override
	protected OrderType processOrderType(OrderWidgetConfig widgetConfig) throws BadFormDesignException {
		OrderType orderType;
		try {
			orderType = super.processOrderType(widgetConfig);
		}
		catch (BadFormDesignException e) {
			List<OrderType> drugOrderTypes = HtmlFormEntryUtil.getDrugOrderTypes();
			if (drugOrderTypes.isEmpty()) {
				throw new BadFormDesignException("There are no order types defined for DrugOrder");
			}
			if (drugOrderTypes.size() > 1) {
				throw new BadFormDesignException("More than one order type exists for DrugOrder.  Please specify orderType");
			}
			orderType = drugOrderTypes.get(0);
			widgetConfig.getOrderField().setOrderType(orderType);
		}
		return orderType;
	}
}

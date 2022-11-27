/*
 *     Copyright © 2022 David Koňařík
 *     This file is part of Inventorysorter.
 *
 *     Inventorysorter is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Inventorysorter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Inventorysorter.  If not, see <http://www.gnu.org/licenses/>.
 */

package cpw.mods.inventorysorter;

import java.util.Comparator;
import java.util.stream.Stream;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import net.minecraftforge.registries.ForgeRegistries;

/**
 * Enum deciding how to order items during sorting
 */
public enum ItemOrdering {
	BY_COUNT,
	BY_MOD_AND_NAME,
	BY_NAME;

	public Stream<Entry<ItemStackHolder>> ordered(Multiset<ItemStackHolder> itemCounts) {
		if (this == BY_COUNT)
			return orderedByCount(itemCounts);
		if (this == BY_MOD_AND_NAME)
			return orderedByModAndName(itemCounts);
		if (this == BY_NAME)
			return orderedByName(itemCounts);
		throw new IllegalStateException();
	}

	private static Stream<Entry<ItemStackHolder>> orderedByCount(Multiset<ItemStackHolder> itemCounts) {
		return itemCounts.entrySet().stream()
				.sorted((a, b) -> {
					int countComp = Integer.compare(a.getCount(), b.getCount());
					if (countComp != 0)
						return -countComp; // Descending
					var itemA = a.getElement().is.getItem();
					var itemB = b.getElement().is.getItem();
					return ForgeRegistries.ITEMS.getKey(itemA).toString()
						.compareTo(ForgeRegistries.ITEMS.getKey(itemB).toString());
				});
	}

	private static Stream<Entry<ItemStackHolder>> orderedByModAndName(Multiset<ItemStackHolder> itemCounts) {
		return itemCounts.entrySet().stream()
				.sorted(Comparator.comparing(e ->
					ForgeRegistries.ITEMS.getKey(e.getElement().is.getItem()).toString()));
	}

	private static Stream<Entry<ItemStackHolder>> orderedByName(Multiset<ItemStackHolder> itemCounts) {
		return itemCounts.entrySet().stream()
				.sorted(Comparator.comparing(e ->
					ForgeRegistries.ITEMS.getKey(e.getElement().is.getItem()).getPath()));
	}
}

//package com.winterwell.moneyscript.output;
//
//import java.util.AbstractList;
//import java.util.Iterator;
//import java.util.List;
//
//import com.winterwell.utils.Environment;
//import com.winterwell.utils.Key;
//
///**
// * Sets an {@link Environment} property as it goes.
// * @author daniel
// *
// * @param <T>
// */
//public class ContextAlteringList<T> extends AbstractList<T> {
//
//	public ContextAlteringList(List<T> list, Key<T> key) {
//
//		this.list = list;
//		this.key = key;
//	}
//
//	final List<T> list;
//	final Key<T> key;
//	
//	@Override
//	public T get(int index) {
//		return list.get(index);
//	}
//
//	@Override
//	public int size() {
//		return list.size();
//	}			
//	
//	@Override
//	public Iterator<T> iterator() {				
//		return new ContextAlteringIterator();
//	}
//	
//	class ContextAlteringIterator implements Iterator<T> {
//		Iterator<T> it = list.iterator();
//		Environment env = Environment.get();
//		@Override
//		public boolean hasNext() {
//			return it.hasNext();
//		}
//
//		@Override
//		public T next() {
//			T n = it.next();
//			env.put(key, n);
//			return n;
//		}
//
//		@Override
//		public void remove() {
//			it.remove();
//		}
//		
//	}
//
//}
//

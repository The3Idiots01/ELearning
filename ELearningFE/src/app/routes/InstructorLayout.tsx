import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { InstructorSidebar } from '../../components/layout/InstructorSidebar';
import { categoryApi } from '../../features/category/api/categoryApi';
import type { Category } from '../../types/category';

export interface InstructorOutletContext {
  categories: Category[];
}

export function InstructorLayout() {
  const [categories, setCategories] = useState<Category[]>([]);

  useEffect(() => {
    categoryApi.getAll().then((cats) => setCategories(cats));
  }, []);

  return (
    <div className="flex-1 flex h-[calc(100vh-36px)] overflow-hidden">
      <InstructorSidebar />
      <main className="flex-1 flex flex-col bg-background overflow-hidden min-h-0">
        <Outlet context={{ categories } satisfies InstructorOutletContext} />
      </main>
    </div>
  );
}

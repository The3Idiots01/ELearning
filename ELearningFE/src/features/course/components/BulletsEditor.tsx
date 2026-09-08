import React from 'react';

interface BulletsEditorProps {
  requirements: string[];
  onChangeRequirements: (items: string[]) => void;
  targetAudiences: string[];
  onChangeTargetAudiences: (items: string[]) => void;
}

export const BulletsEditor: React.FC<BulletsEditorProps> = ({
  requirements,
  onChangeRequirements,
  targetAudiences,
  onChangeTargetAudiences
}) => {
  const change = (list: string[], index: number, value: string, setter: (items: string[]) => void) => {
    const next = [...list];
    next[index] = value;
    setter(next);
  };
  const add = (list: string[], setter: (items: string[]) => void) => setter([...list, '']);
  const remove = (list: string[], index: number, setter: (items: string[]) => void) =>
    setter(list.length <= 1 ? [''] : list.filter((_, itemIndex) => itemIndex !== index));

  const renderList = (
    title: string,
    icon: string,
    hint: string,
    list: string[],
    setter: (items: string[]) => void,
    placeholder: string
  ) => (
    <div className="space-y-3">
      <div className="flex justify-between items-center">
        <div>
          <h4 className="text-sm font-bold text-slate-900 m-0 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[20px]">{icon}</span>
            <span>{title}</span>
          </h4>
          <p className="text-xs text-slate-500 m-0 mt-0.5">{hint}</p>
        </div>
        <span className={`text-xs font-bold px-2.5 py-1 rounded-full border ${list.filter((item) => item.trim()).length > 0 ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-amber-50 text-amber-700 border-amber-200'}`}>
          {list.filter((item) => item.trim()).length}/1 mục
        </span>
      </div>
      <div className="space-y-2">
        {list.map((item, index) => (
          <div key={index} className="flex items-center gap-2">
            <span className="text-xs font-bold text-slate-400 w-6 text-right shrink-0">{index + 1}.</span>
            <input
              value={item}
              onChange={(event) => change(list, index, event.target.value, setter)}
              placeholder={placeholder}
              className="flex-1 px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
            />
            <button type="button" onClick={() => remove(list, index, setter)} className="p-2 text-slate-400 hover:text-rose-600 rounded-xl cursor-pointer" title="Xóa mục">
              <span className="material-symbols-outlined text-[18px]">delete</span>
            </button>
          </div>
        ))}
      </div>
      <button type="button" onClick={() => add(list, setter)} className="text-primary font-bold text-xs inline-flex items-center gap-1.5 cursor-pointer">
        <span className="material-symbols-outlined text-[18px]">add</span> Thêm mục
      </button>
    </div>
  );

  return (
    <div className="space-y-8">
      {renderList('Yêu cầu đầu vào (Kiến thức / Thiết bị cần có)', 'verified', 'Tối thiểu 1 yêu cầu.', requirements, onChangeRequirements, 'Ví dụ: Biết Java cơ bản hoặc có máy tính kết nối Internet...')}
      <div className="pt-6 border-t border-slate-100">
        {renderList('Đối tượng mục tiêu (Khóa học này dành cho ai?)', 'group', 'Tối thiểu 1 đối tượng.', targetAudiences, onChangeTargetAudiences, 'Ví dụ: Sinh viên CNTT, lập trình viên muốn học nâng cao...')}
      </div>
    </div>
  );
};

import React from 'react';

interface BulletsEditorProps {
  learningObjectives: string[];
  onChangeLearningObjectives: (items: string[]) => void;
  requirements: string[];
  onChangeRequirements: (items: string[]) => void;
  targetAudiences: string[];
  onChangeTargetAudiences: (items: string[]) => void;
}

export const BulletsEditor: React.FC<BulletsEditorProps> = ({
  learningObjectives,
  onChangeLearningObjectives,
  requirements,
  onChangeRequirements,
  targetAudiences,
  onChangeTargetAudiences
}) => {
  // Helper for dynamic bullet lists
  const handleItemChange = (
    list: string[],
    index: number,
    value: string,
    setter: (items: string[]) => void
  ) => {
    const updated = [...list];
    updated[index] = value;
    setter(updated);
  };

  const handleAddItem = (list: string[], setter: (items: string[]) => void) => {
    setter([...list, '']);
  };

  const handleRemoveItem = (
    list: string[],
    index: number,
    setter: (items: string[]) => void
  ) => {
    if (list.length <= 1) {
      setter(['']);
      return;
    }
    setter(list.filter((_, i) => i !== index));
  };

  return (
    <div className="space-y-8">
      {/* 1. Learning Objectives */}
      <div className="space-y-3">
        <div className="flex justify-between items-center">
          <div>
            <h4 className="text-sm font-bold text-slate-900 m-0 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">
                check_circle
              </span>
              <span>Mục tiêu học tập (Học viên sẽ học được gì?)</span>
            </h4>
            <p className="text-xs text-slate-500 m-0 mt-0.5">
              Cần ít nhất <strong className="text-primary font-bold">4 mục tiêu rõ ràng</strong> để đủ điều kiện xuất bản (Publish Check).
            </p>
          </div>
          <span
            className={`text-xs font-bold px-2.5 py-1 rounded-full border ${
              learningObjectives.filter((o) => o.trim().length > 0).length >= 4
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-amber-50 text-amber-700 border-amber-200'
            }`}
          >
            {learningObjectives.filter((o) => o.trim().length > 0).length}/4 mục
          </span>
        </div>

        <div className="space-y-2">
          {learningObjectives.map((item, idx) => (
            <div key={idx} className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-400 w-6 text-right shrink-0">
                {idx + 1}.
              </span>
              <input
                type="text"
                value={item}
                onChange={(e) =>
                  handleItemChange(learningObjectives, idx, e.target.value, onChangeLearningObjectives)
                }
                placeholder={`Mục tiêu cụ thể ${idx + 1} (vd: Xây dựng REST API hoàn chỉnh với Spring Boot)...`}
                className="flex-1 px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all"
              />
              <button
                type="button"
                onClick={() =>
                  handleRemoveItem(learningObjectives, idx, onChangeLearningObjectives)
                }
                className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer shrink-0"
                title="Xóa mục"
              >
                <span className="material-symbols-outlined text-[18px]">delete</span>
              </button>
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={() => handleAddItem(learningObjectives, onChangeLearningObjectives)}
          className="text-primary hover:text-primary-container font-bold text-xs inline-flex items-center gap-1.5 pt-1 cursor-pointer"
        >
          <span className="material-symbols-outlined text-[18px]">add</span>
          <span>Thêm mục tiêu học tập</span>
        </button>
      </div>

      {/* 2. Requirements */}
      <div className="space-y-3 pt-6 border-t border-slate-100">
        <div className="flex justify-between items-center">
          <div>
            <h4 className="text-sm font-bold text-slate-900 m-0 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">
                verified
              </span>
              <span>Yêu cầu đầu vào (Kiến thức / Thiết bị cần có)</span>
            </h4>
            <p className="text-xs text-slate-500 m-0 mt-0.5">
              Tối thiểu <strong className="text-primary font-bold">1 yêu cầu</strong>.
            </p>
          </div>
          <span
            className={`text-xs font-bold px-2.5 py-1 rounded-full border ${
              requirements.filter((r) => r.trim().length > 0).length >= 1
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-amber-50 text-amber-700 border-amber-200'
            }`}
          >
            {requirements.filter((r) => r.trim().length > 0).length}/1 mục
          </span>
        </div>

        <div className="space-y-2">
          {requirements.map((item, idx) => (
            <div key={idx} className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-400 w-6 text-right shrink-0">
                {idx + 1}.
              </span>
              <input
                type="text"
                value={item}
                onChange={(e) =>
                  handleItemChange(requirements, idx, e.target.value, onChangeRequirements)
                }
                placeholder={`Yêu cầu ${idx + 1} (vd: Biết Java cơ bản hoặc có máy tính kết nối Internet)...`}
                className="flex-1 px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all"
              />
              <button
                type="button"
                onClick={() => handleRemoveItem(requirements, idx, onChangeRequirements)}
                className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer shrink-0"
                title="Xóa mục"
              >
                <span className="material-symbols-outlined text-[18px]">delete</span>
              </button>
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={() => handleAddItem(requirements, onChangeRequirements)}
          className="text-primary hover:text-primary-container font-bold text-xs inline-flex items-center gap-1.5 pt-1 cursor-pointer"
        >
          <span className="material-symbols-outlined text-[18px]">add</span>
          <span>Thêm yêu cầu đầu vào</span>
        </button>
      </div>

      {/* 3. Target Audiences */}
      <div className="space-y-3 pt-6 border-t border-slate-100">
        <div className="flex justify-between items-center">
          <div>
            <h4 className="text-sm font-bold text-slate-900 m-0 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">
                group
              </span>
              <span>Đối tượng mục tiêu (Khóa học này dành cho ai?)</span>
            </h4>
            <p className="text-xs text-slate-500 m-0 mt-0.5">
              Tối thiểu <strong className="text-primary font-bold">1 đối tượng</strong>.
            </p>
          </div>
          <span
            className={`text-xs font-bold px-2.5 py-1 rounded-full border ${
              targetAudiences.filter((a) => a.trim().length > 0).length >= 1
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-amber-50 text-amber-700 border-amber-200'
            }`}
          >
            {targetAudiences.filter((a) => a.trim().length > 0).length}/1 mục
          </span>
        </div>

        <div className="space-y-2">
          {targetAudiences.map((item, idx) => (
            <div key={idx} className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-400 w-6 text-right shrink-0">
                {idx + 1}.
              </span>
              <input
                type="text"
                value={item}
                onChange={(e) =>
                  handleItemChange(targetAudiences, idx, e.target.value, onChangeTargetAudiences)
                }
                placeholder={`Đối tượng ${idx + 1} (vd: Sinh viên CNTT, Lập trình viên muốn học nâng cao)...`}
                className="flex-1 px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all"
              />
              <button
                type="button"
                onClick={() => handleRemoveItem(targetAudiences, idx, onChangeTargetAudiences)}
                className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer shrink-0"
                title="Xóa mục"
              >
                <span className="material-symbols-outlined text-[18px]">delete</span>
              </button>
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={() => handleAddItem(targetAudiences, onChangeTargetAudiences)}
          className="text-primary hover:text-primary-container font-bold text-xs inline-flex items-center gap-1.5 pt-1 cursor-pointer"
        >
          <span className="material-symbols-outlined text-[18px]">add</span>
          <span>Thêm đối tượng mục tiêu</span>
        </button>
      </div>
    </div>
  );
};

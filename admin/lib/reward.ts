/**
 * Helper dùng chung cho các form có trường rewardJson.
 * Đảm bảo giá trị gửi đi là chuỗi JSON hợp lệ.
 */
export function validateRewardJson(value: any): string {
  if (value == null || value === "") return "";
  if (typeof value !== "string") return JSON.stringify(value);
  try {
    JSON.parse(value);
  } catch {
    throw new Error("rewardJson không phải JSON hợp lệ");
  }
  return value;
}

export const REWARD_HINT =
  'Định dạng: {"xu":..,"gold":..,"items":[{"itemId":..,"qty":..}]}';

export const REWARD_SAMPLE =
  '{"xu":1000,"gold":50000,"items":[{"itemId":1,"qty":10}]}';

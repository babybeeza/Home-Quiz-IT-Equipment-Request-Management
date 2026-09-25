/** Native suggestions for the free-text department inputs; renders nothing visible. */
export function DepartmentOptions({ id, names }: { id: string; names: string[] }) {
  return <datalist id={id}>{names.map((name) => <option key={name} value={name} />)}</datalist>;
}

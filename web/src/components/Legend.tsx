import "./legend.css";

type LegendItem = {
    label: string;
    className: string;
};

type LegendProps = {
    items: LegendItem[];
};

export default function Legend({ items }: LegendProps) {
    return (
        <div className="legend">
            {items.map((item) => (
                <div key={item.label} className="legend-item">
                    <span className={`legend-box ${item.className}`}></span>
                    {item.label}
                </div>
            ))}
        </div>
    );
}